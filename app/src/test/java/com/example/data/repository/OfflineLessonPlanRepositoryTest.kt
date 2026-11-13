package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.AppDatabase
import com.example.domain.model.LessonPlan
import com.example.util.ConnectivityObserver
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
import org.junit.Before
import org.junit.Test
import java.util.UUID

private class FakeConnectivityObserver(initialOnline: Boolean) : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    override fun isCurrentlyOnline(): Boolean = _isOnline.value
    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

class OfflineLessonPlanRepositoryTest : FirestoreEmulatorTestBase() {

    private lateinit var localDb: AppDatabase

    @Before
    fun initRoom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        localDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeRoom() {
        localDb.close()
    }

    @Test
    fun downloadEditOffline_andSyncOnReconnect_updatesFirestore() = runBlocking {
        val uid = signInTestUser("offline_teacher_${UUID.randomUUID()}@test.com")
        val remoteRepo = LessonPlanRepository(firestore)
        val connectivity = FakeConnectivityObserver(initialOnline = true)
        val offlineRepo = OfflineLessonPlanRepository(
            dao = localDb.offlineLessonPlanDao(),
            remoteRepo = remoteRepo,
            connectivity = connectivity
        )

        // 1. Create a lesson plan online
        val original = LessonPlan(
            userId = uid,
            authorName = "Bikash Mohanty",
            title = "ସ୍ୱରବର୍ଣ୍ଣ ଅଭ୍ୟାସ",
            stage = "foundational",
            classLevel = "Class 1 (ପ୍ରଥମ)",
            subject = "ମାତୃଭାଷା ଓଡ଼ିଆ",
            medium = "Odia",
            topic = "ଅ ଆ ଇ ଈ",
            durationMinutes = 40L,
            planDay = 12L,
            planMonth = 11L,
            planYear = 2026L,
            aditiStep = "Initial Aditi step",
            status = "draft"
        )
        val planId = withTimeout(5000L) {
            remoteRepo.createLessonPlan(original).getOrThrow()
        }
        val savedRemote = withTimeout(5000L) {
            remoteRepo.getLessonPlanById(planId).getOrThrow()!!
        }

        // 2. Download to device for offline access
        offlineRepo.downloadForOffline(savedRemote).getOrThrow()
        val downloaded = offlineRepo.observeDownloadedPlans().first()
        assertEquals(1, downloaded.size)
        assertEquals(planId, downloaded.first().id)

        // 3. Go offline and edit the downloaded plan
        connectivity.setOnline(false)
        val editedOffline = savedRemote.copy(
            title = "ସ୍ୱରବର୍ଣ୍ଣ ଓ ମାତ୍ରା ଅଭ୍ୟାସ (Edited Offline)",
            aditiStep = "Updated offline Aditi activity"
        )
        val offlineSaveRes = offlineRepo.savePlanOfflineFirst(
            plan = editedOffline,
            currentUserId = uid,
            authorName = "Bikash Mohanty"
        ).getOrThrow()

        assertFalse(offlineSaveRes.syncedImmediately)
        assertEquals(1, offlineRepo.observePendingSyncCount(uid).first())

        // Verify stale remote snapshot does NOT overwrite pending local edits
        offlineRepo.refreshDownloadedCopies(listOf(savedRemote))
        val localAfterRefresh = offlineRepo.observeDownloadedPlans().first().first()
        assertEquals("Updated offline Aditi activity", localAfterRefresh.aditiStep)

        // 4. Restore connection and sync pending edits to Firestore
        connectivity.setOnline(true)
        val syncSummary = withTimeout(5000L) {
            offlineRepo.syncPendingPlans(uid)
        }
        assertEquals(1, syncSummary.syncedCount)
        assertEquals(0, syncSummary.failedCount)
        assertEquals(0, offlineRepo.observePendingSyncCount(uid).first())

        // 5. Verify Firestore now holds the offline edit
        val remoteAfterSync = withTimeout(5000L) {
            remoteRepo.getLessonPlanById(planId).getOrThrow()!!
        }
        assertEquals("ସ୍ୱରବର୍ଣ୍ଣ ଓ ମାତ୍ରା ଅଭ୍ୟାସ (Edited Offline)", remoteAfterSync.title)
        assertEquals("Updated offline Aditi activity", remoteAfterSync.aditiStep)
    }

    @Test
    fun createPlanOffline_andSyncOnReconnect_createsInFirestore() = runBlocking {
        val uid = signInTestUser("offline_creator_${UUID.randomUUID()}@test.com")
        val remoteRepo = LessonPlanRepository(firestore)
        val connectivity = FakeConnectivityObserver(initialOnline = false)
        val offlineRepo = OfflineLessonPlanRepository(
            dao = localDb.offlineLessonPlanDao(),
            remoteRepo = remoteRepo,
            connectivity = connectivity
        )

        val draftOffline = LessonPlan(
            userId = uid,
            authorName = "Pravati Sahoo",
            title = "ଭଗ୍ନାଂଶ ପରିଚୟ (Offline Created)",
            stage = "preparatory",
            classLevel = "Class 4 (ଚତୁର୍ଥ)",
            subject = "ଗଣିତ (Mathematics)",
            medium = "Odia",
            topic = "ଅଧା ଓ ଚଉଠ",
            durationMinutes = 45L,
            planDay = 12L,
            planMonth = 11L,
            planYear = 2026L,
            status = "draft"
        )

        val saveRes = offlineRepo.savePlanOfflineFirst(draftOffline, uid, "Pravati Sahoo").getOrThrow()
        assertFalse(saveRes.syncedImmediately)
        assertTrue(saveRes.plan.id.isNotBlank())
        assertEquals(1, offlineRepo.observePendingSyncCount(uid).first())

        // Restore connection and sync
        connectivity.setOnline(true)
        val summary = withTimeout(5000L) {
            offlineRepo.syncPendingPlans(uid)
        }
        assertEquals(1, summary.syncedCount)
        assertEquals(0, summary.failedCount)
        assertEquals(0, offlineRepo.observePendingSyncCount(uid).first())

        val remoteDoc = withTimeout(5000L) {
            remoteRepo.getLessonPlanById(saveRes.plan.id).getOrThrow()
        }
        assertEquals("ଭଗ୍ନାଂଶ ପରିଚୟ (Offline Created)", remoteDoc?.title)
    }
}
