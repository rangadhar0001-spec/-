package com.example.data.repository

import android.util.Log
import com.example.data.local.LessonProgressDao
import com.example.data.local.SyncStatus
import com.example.data.local.toEntity
import com.example.domain.model.LessonPlanProgress
import com.example.domain.model.NepStage
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.util.ConnectivityObserver
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

data class SaveProgressResult(
    val progress: LessonPlanProgress,
    val syncedImmediately: Boolean
)

class LessonProgressRepository(
    private val db: FirebaseFirestore,
    private val dao: LessonProgressDao? = null,
    private val connectivity: ConnectivityObserver? = null
) {
    private val syncMutex = Mutex()

    fun observeProgressForUser(userId: String): Flow<List<LessonPlanProgress>> {
        val remoteFlow = db.collection("users").document(userId)
            .collection("lesson_progress")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LessonPlanProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .onEach { remoteList ->
                if (dao != null && remoteList.isNotEmpty()) {
                    val toCache = remoteList.mapNotNull { remote ->
                        val local = dao.getByPlanId(remote.lessonPlanId)
                        if (local == null || local.syncStatus == SyncStatus.SYNCED.name) {
                            remote.toEntity(SyncStatus.SYNCED)
                        } else {
                            null
                        }
                    }
                    if (toCache.isNotEmpty()) {
                        dao.upsertAll(toCache)
                    }
                }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (dao == null) {
                    if (Firebase.auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "users/$userId/lesson_progress")
                    }
                    throw error
                } else {
                    Log.w(TAG, "Remote progress observe fallback to Room", fsErr ?: error)
                    emit(emptyList())
                }
            }

        val localFlow = dao?.observeProgressForUser(userId)?.map { list -> list.map { it.toDomain() } }
            ?: flowOf(emptyList())

        return if (dao == null) {
            remoteFlow
        } else {
            combine(remoteFlow, localFlow) { remote, local ->
                val merged = LinkedHashMap<String, LessonPlanProgress>()
                val online = connectivity?.isCurrentlyOnline() ?: true
                for (item in local) {
                    val entity = dao.getByPlanId(item.lessonPlanId)
                    if (!online || (entity != null && entity.syncStatus != SyncStatus.SYNCED.name)) {
                        merged[item.lessonPlanId] = item
                    }
                }
                for (item in remote) {
                    if (item.lessonPlanId !in merged) {
                        merged[item.lessonPlanId] = item
                    }
                }
                for (item in local) {
                    if (item.lessonPlanId !in merged) {
                        merged[item.lessonPlanId] = item
                    }
                }
                merged.values.toList()
            }
        }
    }

    fun observePendingCount(userId: String): Flow<Int> =
        dao?.observePendingCount(userId) ?: flowOf(0)

    suspend fun saveProgress(progress: LessonPlanProgress): Result<SaveProgressResult> = runCatching {
        val uid = progress.userId.ifBlank {
            Firebase.auth.currentUser?.uid ?: error("Authenticated user required")
        }
        val planId = progress.lessonPlanId.ifBlank { progress.id }
        require(planId.isNotBlank()) { "Lesson plan ID required to track progress" }

        val normalized = progress.copy(
            id = planId,
            userId = uid,
            lessonPlanId = planId,
            stage = NepStage.fromKey(progress.stage).key,
            checkedGuidelineIds = progress.checkedGuidelineIds.distinct().take(20),
            achievedOutcomeIndices = progress.achievedOutcomeIndices.distinct().take(30),
            inProgressOutcomeIndices = progress.inProgressOutcomeIndices
                .filterNot { it in progress.achievedOutcomeIndices }
                .distinct()
                .take(30),
            classroomNotes = progress.classroomNotes.take(1500),
            lastTrackedDay = progress.lastTrackedDay.coerceIn(1L, 31L),
            lastTrackedMonth = progress.lastTrackedMonth.coerceIn(1L, 12L),
            lastTrackedYear = progress.lastTrackedYear.coerceIn(2020L, 2100L),
            completionPercent = progress.completionPercent.coerceIn(0L, 100L)
        )

        // Save locally first if Room DAO is wired
        dao?.upsert(normalized.toEntity(syncStatus = SyncStatus.PENDING_UPDATE))

        val isOnline = connectivity?.isCurrentlyOnline() ?: true
        if (!isOnline) {
            return@runCatching SaveProgressResult(normalized, syncedImmediately = false)
        }

        val remoteResult = pushToFirestore(normalized)
        if (remoteResult.isSuccess) {
            dao?.upsert(normalized.toEntity(syncStatus = SyncStatus.SYNCED))
            SaveProgressResult(normalized, syncedImmediately = true)
        } else {
            val err = remoteResult.exceptionOrNull()
            if (err is CancellationException) throw err
            if (dao == null) throw err ?: IllegalStateException("Failed to save progress")
            Log.w(TAG, "Progress save queued offline for plan=$planId", err)
            SaveProgressResult(normalized, syncedImmediately = false)
        }
    }

    suspend fun syncPendingProgress(userId: String): Int = syncMutex.withLock {
        val localDao = dao ?: return@withLock 0
        if (connectivity?.isCurrentlyOnline() == false) return@withLock 0
        val pending = localDao.getPendingForUser(userId)
        var synced = 0
        for (entity in pending) {
            if (connectivity?.isCurrentlyOnline() == false) break
            val res = pushToFirestore(entity.toDomain())
            if (res.isSuccess) {
                localDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED.name))
                synced++
            }
        }
        synced
    }

    private suspend fun pushToFirestore(progress: LessonPlanProgress): Result<Unit> = runCatching {
        val uid = progress.userId
        val planId = progress.lessonPlanId
        val docRef = db.collection("users").document(uid)
            .collection("lesson_progress").document(planId)
        val snap = docRef.get().await()

        val updateFields = mapOf(
            "stage" to NepStage.fromKey(progress.stage).key,
            "aditiCompleted" to progress.aditiCompleted,
            "bodhaCompleted" to progress.bodhaCompleted,
            "abhyasaCompleted" to progress.abhyasaCompleted,
            "prayogaCompleted" to progress.prayogaCompleted,
            "prasaraCompleted" to progress.prasaraCompleted,
            "checkedGuidelineIds" to progress.checkedGuidelineIds,
            "achievedOutcomeIndices" to progress.achievedOutcomeIndices,
            "inProgressOutcomeIndices" to progress.inProgressOutcomeIndices,
            "inclusiveSupportApplied" to progress.inclusiveSupportApplied,
            "tlmUsedInClass" to progress.tlmUsedInClass,
            "assessmentConducted" to progress.assessmentConducted,
            "classroomNotes" to progress.classroomNotes.take(1500),
            "lastTrackedDay" to progress.lastTrackedDay,
            "lastTrackedMonth" to progress.lastTrackedMonth,
            "lastTrackedYear" to progress.lastTrackedYear,
            "completionPercent" to progress.completionPercent,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (snap.exists()) {
            docRef.update(updateFields).await()
        } else {
            val createFields = updateFields + mapOf(
                "userId" to uid,
                "lessonPlanId" to planId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createFields).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${progress.userId}/lesson_progress/${progress.lessonPlanId}")
    }

    private companion object {
        const val TAG = "LessonProgressRepo"
    }
}
