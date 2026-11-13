package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.model.LessonPlan
import com.example.domain.model.PanchapadiConfig
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LessonPlanRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeMyLessonPlans(userId: String): Flow<List<LessonPlan>> =
        db.collection("lesson_plans")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LessonPlan::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "lesson_plans")
                }
                throw error
            }

    fun observePublishedLessonPlans(): Flow<List<LessonPlan>> =
        db.collection("lesson_plans")
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LessonPlan::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "lesson_plans")
                throw error
            }

    fun observePanchapadiConfig(): Flow<PanchapadiConfig> =
        db.collection("app_config").document("panchapadi")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PanchapadiConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PanchapadiConfig()
                } else {
                    PanchapadiConfig()
                }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(PanchapadiConfig())
            }

    suspend fun getLessonPlanById(planId: String): Result<LessonPlan?> = runCatching {
        val snap = db.collection("lesson_plans").document(planId).get().await()
        if (snap.exists()) {
            snap.toObject(LessonPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
            null
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "lesson_plans/$planId")
    }

    suspend fun createLessonPlan(plan: LessonPlan): Result<String> = runCatching {
        val uid = plan.userId.ifBlank {
            Firebase.auth.currentUser?.uid ?: error("Authenticated user required")
        }
        val docRef = if (plan.id.isNotBlank()) {
            db.collection("lesson_plans").document(plan.id)
        } else {
            db.collection("lesson_plans").document()
        }

        val payload = mapOf(
            "userId" to uid,
            "authorName" to plan.authorName.ifBlank { "Educator" }.take(100),
            "title" to plan.title.ifBlank { plan.topic.ifBlank { "Untitled Lesson Plan" } }.take(200),
            "stage" to plan.stage,
            "classLevel" to plan.classLevel.take(40),
            "subject" to plan.subject.take(80),
            "medium" to plan.medium.take(40),
            "topic" to plan.topic.ifBlank { plan.title }.take(200),
            "durationMinutes" to plan.durationMinutes.coerceIn(5L, 300L),
            "planDay" to plan.planDay.coerceIn(1L, 31L),
            "planMonth" to plan.planMonth.coerceIn(1L, 12L),
            "planYear" to plan.planYear.coerceIn(2020L, 2100L),
            "learningObjectives" to plan.learningObjectives.take(3000),
            "tlmMaterials" to plan.tlmMaterials.take(2000),
            "teacherActivities" to plan.teacherActivities.take(3000),
            "studentActivities" to plan.studentActivities.take(3000),
            "aditiStep" to plan.aditiStep.take(3000),
            "aditiMinutes" to plan.aditiMinutes.coerceIn(1L, 120L),
            "bodhaStep" to plan.bodhaStep.take(4000),
            "bodhaMinutes" to plan.bodhaMinutes.coerceIn(1L, 120L),
            "abhyasaStep" to plan.abhyasaStep.take(3000),
            "abhyasaMinutes" to plan.abhyasaMinutes.coerceIn(1L, 120L),
            "prayogaStep" to plan.prayogaStep.take(3000),
            "prayogaMinutes" to plan.prayogaMinutes.coerceIn(1L, 120L),
            "prasaraStep" to plan.prasaraStep.take(3000),
            "prasaraMinutes" to plan.prasaraMinutes.coerceIn(1L, 120L),
            "competencyQuestions" to plan.competencyQuestions.take(3000),
            "assessmentCriteria" to plan.assessmentCriteria.take(3000),
            "homeworkActivities" to plan.homeworkActivities.take(2000),
            "inclusiveSuggestions" to plan.inclusiveSuggestions.take(2000),
            "summary" to plan.summary.take(2000),
            "teacherReflection" to plan.teacherReflection.take(2000),
            "isAiGenerated" to plan.isAiGenerated,
            "isVerified" to false,
            "status" to (if (plan.status in listOf("draft", "published", "archived")) plan.status else "draft"),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "lesson_plans")
    }

    suspend fun updateLessonPlan(plan: LessonPlan): Result<Unit> = runCatching {
        require(plan.id.isNotBlank()) { "Plan ID required for update" }
        val docRef = db.collection("lesson_plans").document(plan.id)
        val payload = mapOf(
            "authorName" to plan.authorName.ifBlank { "Educator" }.take(100),
            "title" to plan.title.ifBlank { plan.topic }.take(200),
            "stage" to plan.stage,
            "classLevel" to plan.classLevel.take(40),
            "subject" to plan.subject.take(80),
            "medium" to plan.medium.take(40),
            "topic" to plan.topic.take(200),
            "durationMinutes" to plan.durationMinutes.coerceIn(5L, 300L),
            "planDay" to plan.planDay.coerceIn(1L, 31L),
            "planMonth" to plan.planMonth.coerceIn(1L, 12L),
            "planYear" to plan.planYear.coerceIn(2020L, 2100L),
            "learningObjectives" to plan.learningObjectives.take(3000),
            "tlmMaterials" to plan.tlmMaterials.take(2000),
            "teacherActivities" to plan.teacherActivities.take(3000),
            "studentActivities" to plan.studentActivities.take(3000),
            "aditiStep" to plan.aditiStep.take(3000),
            "aditiMinutes" to plan.aditiMinutes.coerceIn(1L, 120L),
            "bodhaStep" to plan.bodhaStep.take(4000),
            "bodhaMinutes" to plan.bodhaMinutes.coerceIn(1L, 120L),
            "abhyasaStep" to plan.abhyasaStep.take(3000),
            "abhyasaMinutes" to plan.abhyasaMinutes.coerceIn(1L, 120L),
            "prayogaStep" to plan.prayogaStep.take(3000),
            "prayogaMinutes" to plan.prayogaMinutes.coerceIn(1L, 120L),
            "prasaraStep" to plan.prasaraStep.take(3000),
            "prasaraMinutes" to plan.prasaraMinutes.coerceIn(1L, 120L),
            "competencyQuestions" to plan.competencyQuestions.take(3000),
            "assessmentCriteria" to plan.assessmentCriteria.take(3000),
            "homeworkActivities" to plan.homeworkActivities.take(2000),
            "inclusiveSuggestions" to plan.inclusiveSuggestions.take(2000),
            "summary" to plan.summary.take(2000),
            "teacherReflection" to plan.teacherReflection.take(2000),
            "isAiGenerated" to plan.isAiGenerated,
            "status" to (if (plan.status in listOf("draft", "published", "archived")) plan.status else "draft"),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "lesson_plans/${plan.id}")
    }

    suspend fun deleteLessonPlan(planId: String): Result<Unit> = runCatching {
        db.collection("lesson_plans").document(planId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "lesson_plans/$planId")
    }

    suspend fun adminVerifyLessonPlan(planId: String, verified: Boolean): Result<Unit> = runCatching {
        db.collection("lesson_plans").document(planId).update(
            mapOf(
                "isVerified" to verified,
                "status" to if (verified) "published" else "draft",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "lesson_plans/$planId")
    }

    suspend fun savePanchapadiConfig(config: PanchapadiConfig, adminUid: String): Result<Unit> = runCatching {
        val payload = mapOf(
            "isActive" to true,
            "aditiLabel" to config.aditiLabel.take(80),
            "aditiDesc" to config.aditiDesc.take(400),
            "bodhaLabel" to config.bodhaLabel.take(80),
            "bodhaDesc" to config.bodhaDesc.take(400),
            "abhyasaLabel" to config.abhyasaLabel.take(80),
            "abhyasaDesc" to config.abhyasaDesc.take(400),
            "prayogaLabel" to config.prayogaLabel.take(80),
            "prayogaDesc" to config.prayogaDesc.take(400),
            "prasaraLabel" to config.prasaraLabel.take(80),
            "prasaraDesc" to config.prasaraDesc.take(400),
            "updatedBy" to adminUid.take(128),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("app_config").document("panchapadi").set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "app_config/panchapadi")
    }
}
