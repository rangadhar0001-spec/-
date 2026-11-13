package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.CommunityPost
import com.example.domain.model.LessonPlan
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SikshyaSathiRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveLessonPlan_authenticatedOwner_succeedsWithDayMonthYear() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val profileRepo = AuthProfileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            profileRepo.ensureUserProfile(uid, "Sunita Dash", "alice@test.com").getOrThrow()
        }

        val planRepo = LessonPlanRepository(firestore)
        val plan = LessonPlan(
            userId = uid,
            authorName = "Sunita Dash",
            title = "ଓଡ଼ିଆ ବର୍ଣ୍ଣମାଳା ପରିଚୟ",
            stage = "foundational",
            classLevel = "Class 1 (ପ୍ରଥମ)",
            subject = "ମାତୃଭାଷା ଓଡ଼ିଆ",
            medium = "Odia",
            topic = "ବର୍ଣ୍ଣମାଳା",
            durationMinutes = 40L,
            planDay = 12L,
            planMonth = 11L,
            planYear = 2026L,
            learningObjectives = "ସ୍ୱର ଓ ବ୍ୟଞ୍ଜନ ବର୍ଣ୍ଣ ଚିହ୍ନିବେ",
            aditiStep = "ଗୀତ ଗାଇବା",
            aditiMinutes = 8L,
            bodhaStep = "ଅକ୍ଷର କାର୍ଡ ଦେଖାଇବା",
            bodhaMinutes = 12L,
            abhyasaStep = "ସ୍ଲେଟରେ ଲେଖିବା",
            abhyasaMinutes = 10L,
            prayogaStep = "ନାମର ପ୍ରଥମ ଅକ୍ଷର ଖୋଜିବା",
            prayogaMinutes = 5L,
            prasaraStep = "ଘରେ ଅଭ୍ୟାସ",
            prasaraMinutes = 5L,
            status = "draft"
        )

        val planId = withTimeout(DEFAULT_TIMEOUT_MS) {
            planRepo.createLessonPlan(plan).getOrThrow()
        }
        assertTrue(planId.isNotBlank())

        val myPlans = withTimeout(FLOW_TIMEOUT_MS) {
            planRepo.observeMyLessonPlans(uid).first { list -> list.any { it.id == planId } }
        }
        val saved = myPlans.first { it.id == planId }
        assertEquals(12L, saved.planDay)
        assertEquals(11L, saved.planMonth)
        assertEquals(2026L, saved.planYear)
    }

    @Test
    fun crossUserDraftAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_priv_${UUID.randomUUID()}@test.com")
        val planRepo = LessonPlanRepository(firestore)
        val planId = withTimeout(DEFAULT_TIMEOUT_MS) {
            planRepo.createLessonPlan(
                LessonPlan(
                    userId = aliceUid,
                    authorName = "Alice",
                    title = "Private Draft",
                    stage = "preparatory",
                    classLevel = "Class 4",
                    subject = "Mathematics",
                    medium = "Odia",
                    topic = "Geometry",
                    status = "draft"
                )
            ).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                planRepo.getLessonPlanById(planId).getOrThrow()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's draft")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
        }
    }

    @Test
    fun communityPostAndComment_authenticatedUsers_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_post_${UUID.randomUUID()}@test.com")
        val communityRepo = CommunityRepository(firestore)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            communityRepo.createPost(
                CommunityPost(
                    userId = aliceUid,
                    authorName = "Madhusmita Swain",
                    authorRole = "teacher",
                    authorSchool = "Ravenshaw Collegiate",
                    title = "Class 6 Science Experiment",
                    content = "Demonstrating magnets using local materials",
                    stage = "middle",
                    classLevel = "Class 6",
                    subject = "Science",
                    district = "Cuttack (କଟକ)",
                    planDay = 12L,
                    planMonth = 11L,
                    planYear = 2026L
                )
            ).getOrThrow()
        }

        val bobUid = signInTestUser("bob_comment_${UUID.randomUUID()}@test.com")
        val savedPost = withTimeout(FLOW_TIMEOUT_MS) {
            communityRepo.observePublicFeed().first { list -> list.any { it.id == postId } }
        }.first { it.id == postId }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            communityRepo.addComment(savedPost, bobUid, "Prakash Behera", "Excellent idea!").getOrThrow()
        }
        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            communityRepo.observeComments(postId).first { it.isNotEmpty() }
        }
        assertTrue(comments.any { it.text == "Excellent idea!" })
    }

    @Test
    fun observeMyLessonPlans_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val planRepo = LessonPlanRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                planRepo.observeMyLessonPlans("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
