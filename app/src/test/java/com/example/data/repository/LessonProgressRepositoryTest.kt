package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.AppDatabase
import com.example.domain.model.LessonPlan
import com.example.domain.model.LessonPlanProgress
import com.example.domain.model.NepProgressEvaluator
import com.example.domain.model.NepStage
import com.example.util.ConnectivityObserver
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

private class TestProgressConnectivity(initialOnline: Boolean) : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    override fun isCurrentlyOnline(): Boolean = _isOnline.value
    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

class LessonProgressRepositoryTest : FirestoreEmulatorTestBase() {

    private lateinit var localDb: AppDatabase

    @Before
    fun setupRoom() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        val context = ApplicationProvider.getApplicationContext<Context>()
        localDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownRoom() {
        localDb.close()
    }

    @Test
    fun evaluatorComputesReadinessCompletionAndSummaryAccurately() {
        val plan = LessonPlan(
            id = "plan_eval_1",
            userId = "u1",
            title = "ଭଗ୍ନାଂଶ",
            stage = NepStage.PREPARATORY.key,
            topic = "Fractions",
            learningObjectives = "• ପିଲାମାନେ ଅଧା ଓ ଚଉଠ ବୁଝିପାରିବେ\n• ଦୈନନ୍ଦିନ ଜୀବନରେ ଭଗ୍ନାଂଶ ବ୍ୟବହାର କରିବେ",
            tlmMaterials = "କାଗଜ ପଟି ଓ ଫଳ",
            aditiStep = "ରୋଟି ବାଣ୍ଟିବା ଉଦାହରଣ ଦେବା",
            bodhaStep = "ଚିତ୍ର ଦ୍ୱାରା ୧/୨ ଓ ୧/୪ ବୁଝାଇବା",
            abhyasaStep = "ଦଳଗତ ଅଭ୍ୟାସ",
            prayogaStep = "ବଜାର ହିସାବ",
            prasaraStep = "ଘରେ ଅଭ୍ୟାସ",
            competencyQuestions = "ଗୋଟିଏ ରୋଟିକୁ ୪ ଭାଗ କଲେ ପ୍ରତି ଭାଗ କେତେ?",
            assessmentCriteria = "ଶ୍ରେଣୀ କାର୍ଯ୍ୟପତ୍ର ଯାଞ୍ଚ",
            inclusiveSuggestions = "ସ୍ପର୍ଶକ ପଟି ବ୍ୟବହାର"
        )

        val outcomes = NepProgressEvaluator.extractOutcomes(plan)
        assertEquals(2, outcomes.size)

        val readiness = NepProgressEvaluator.evaluatePlanReadiness(plan)
        assertEquals(100, readiness.readinessPercent)
        assertEquals(5, readiness.panchapadiReadyCount)

        // Preparatory has 3 stage guidelines + 5 Panchapadi + 2 outcomes + 3 pedagogy checks = 13 total units
        val fullProgress = LessonPlanProgress(
            id = plan.id,
            userId = "u1",
            lessonPlanId = plan.id,
            stage = plan.stage,
            aditiCompleted = true,
            bodhaCompleted = true,
            abhyasaCompleted = true,
            prayogaCompleted = true,
            prasaraCompleted = true,
            checkedGuidelineIds = listOf("p1_discovery", "p2_multilingual", "p3_formative"),
            achievedOutcomeIndices = listOf(0L, 1L),
            inclusiveSupportApplied = true,
            tlmUsedInClass = true,
            assessmentConducted = true
        )
        assertEquals(100, NepProgressEvaluator.computeCompletionPercent(plan, fullProgress))

        val summary = NepProgressEvaluator.aggregateSummary(listOf(plan), mapOf(plan.id to fullProgress))
        assertEquals(100, summary.averageExecutionPercent)
        assertEquals(2, summary.achievedOutcomes)
        assertEquals(1, summary.fullyCompletedPlansCount)
    }

    @Test
    fun saveProgressOffline_syncsToFirestoreOnReconnect_andEnforcesOwnerRule() = runBlocking {
        val aliceUid = signInTestUser("progress_alice_${UUID.randomUUID()}@test.com")
        val connectivity = TestProgressConnectivity(initialOnline = false)
        val repo = LessonProgressRepository(
            db = firestore,
            dao = localDb.lessonProgressDao(),
            connectivity = connectivity
        )

        val planId = "plan_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val progress = LessonPlanProgress(
            id = planId,
            userId = aliceUid,
            lessonPlanId = planId,
            stage = NepStage.FOUNDATIONAL.key,
            aditiCompleted = true,
            bodhaCompleted = true,
            abhyasaCompleted = false,
            prayogaCompleted = false,
            prasaraCompleted = false,
            checkedGuidelineIds = listOf("f1_play_toy", "f2_nipun_fln"),
            achievedOutcomeIndices = listOf(0L),
            inProgressOutcomeIndices = listOf(1L),
            inclusiveSupportApplied = true,
            tlmUsedInClass = true,
            assessmentConducted = false,
            classroomNotes = "ପିଲାମାନେ ଖେଳ ମାଧ୍ୟମରେ ଶୀଘ୍ର ଶିଖିଲେ",
            lastTrackedDay = 12L,
            lastTrackedMonth = 11L,
            lastTrackedYear = 2026L,
            completionPercent = 65L
        )

        // 1. Save offline -> queued in Room
        val offlineRes = repo.saveProgress(progress).getOrThrow()
        assertFalse(offlineRes.syncedImmediately)
        assertEquals(1, repo.observePendingCount(aliceUid).first())

        // 2. Restore connection & sync to Firestore
        connectivity.setOnline(true)
        val syncedCount = withTimeout(5000L) { repo.syncPendingProgress(aliceUid) }
        assertEquals(1, syncedCount)
        assertEquals(0, repo.observePendingCount(aliceUid).first())

        // 3. Verify readable from Firestore directly
        val remoteOnlyRepo = LessonProgressRepository(firestore)
        val loaded = withTimeout(5000L) {
            remoteOnlyRepo.observeProgressForUser(aliceUid).first { it.any { p -> p.lessonPlanId == planId } }
        }
        assertTrue(loaded.any { it.lessonPlanId == planId && it.completionPercent == 65L })

        // 4. Cross-user access must fail with PERMISSION_DENIED
        signInTestUser("progress_bob_${UUID.randomUUID()}@test.com")
        try {
            withTimeout(5000L) {
                remoteOnlyRepo.saveProgress(progress.copy(userId = aliceUid)).getOrThrow()
            }
            fail("Expected PERMISSION_DENIED when Bob writes Alice's lesson_progress")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
        }
    }
}
