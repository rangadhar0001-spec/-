package com.example.data.repository

import android.util.Log
import com.example.data.local.OfflineLessonPlanDao
import com.example.data.local.OfflineLessonPlanEntity
import com.example.data.local.SyncStatus
import com.example.data.local.toOfflineEntity
import com.example.domain.model.LessonPlan
import com.example.util.ConnectivityObserver
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class SaveOfflineResult(
    val plan: LessonPlan,
    val syncedImmediately: Boolean,
    val forkedFromAnotherAuthor: Boolean = false
)

data class SyncSummary(
    val syncedCount: Int,
    val failedCount: Int
)

/**
 * Coordinates device-local Room persistence for downloaded Lesson Plans with Firestore sync.
 * Ensures:
 * 1) Teachers can explicitly download plans to their device and access them without internet.
 * 2) Edits or drafts saved offline are queued in Room and never overwritten by stale remote snapshots.
 * 3) Pending offline edits automatically sync to Firestore when connectivity returns.
 */
class OfflineLessonPlanRepository(
    private val dao: OfflineLessonPlanDao,
    private val remoteRepo: LessonPlanRepository,
    private val connectivity: ConnectivityObserver
) {
    private val syncMutex = Mutex()

    fun observeDownloadedPlans(): Flow<List<LessonPlan>> =
        dao.observeDownloadedPlans().map { list -> list.map { it.toDomain() } }

    fun observeUserLocalPlans(userId: String): Flow<List<LessonPlan>> =
        dao.observeUserLocalPlans(userId).map { list -> list.map { it.toDomain() } }

    fun observeDownloadedPublishedPlans(): Flow<List<LessonPlan>> =
        dao.observeDownloadedPublishedPlans().map { list -> list.map { it.toDomain() } }

    fun observeDownloadedPlanIds(): Flow<Set<String>> =
        dao.observeDownloadedPlanIds().map { it.toSet() }

    fun observePendingSyncIds(userId: String): Flow<Set<String>> =
        dao.observePendingSyncIds(userId).map { it.toSet() }

    fun observePendingSyncCount(userId: String): Flow<Int> =
        dao.observePendingSyncCount(userId)

    /**
     * Explicitly downloads a LessonPlan to Room for offline access.
     * Preserves any existing unsynced offline edits instead of overwriting them.
     */
    suspend fun downloadForOffline(plan: LessonPlan): Result<Unit> = runCatching {
        require(plan.id.isNotBlank()) { "Lesson plan must have an ID to download offline" }
        val existing = dao.getById(plan.id)
        if (existing != null && existing.syncStatus != SyncStatus.SYNCED.name) {
            // Preserve unsynced local edits; ensure downloaded flag is enabled
            dao.upsert(existing.copy(isDownloaded = true))
        } else {
            dao.upsert(
                plan.toOfflineEntity(
                    isDownloaded = true,
                    syncStatus = SyncStatus.SYNCED,
                    lastSyncedMillis = System.currentTimeMillis()
                )
            )
        }
        Unit
    }

    /**
     * Removes a downloaded LessonPlan from offline device storage.
     * Returns false if the plan has unsynced offline edits (kept locally until synced).
     */
    suspend fun removeOfflineDownload(planId: String): Result<Boolean> = runCatching {
        val existing = dao.getById(planId) ?: return@runCatching true
        if (existing.syncStatus == SyncStatus.SYNCED.name) {
            dao.deleteById(planId)
            true
        } else {
            // Keep pending sync items until connection is restored and synced
            false
        }
    }

    /**
     * Keeps already-downloaded synced plans up to date when fresh remote data arrives,
     * without overwriting any plan that has pending local offline edits.
     */
    suspend fun refreshDownloadedCopies(remotePlans: List<LessonPlan>) {
        if (remotePlans.isEmpty()) return
        val remoteById = remotePlans.associateBy { it.id }
        val toUpdate = mutableListOf<OfflineLessonPlanEntity>()
        for ((id, remotePlan) in remoteById) {
            val local = dao.getById(id)
            if (local != null && local.isDownloaded && local.syncStatus == SyncStatus.SYNCED.name) {
                toUpdate.add(
                    remotePlan.toOfflineEntity(
                        isDownloaded = true,
                        syncStatus = SyncStatus.SYNCED,
                        lastModifiedLocalMillis = remotePlan.updatedAt?.toDate()?.time
                            ?: local.lastModifiedLocalMillis,
                        lastSyncedMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        if (toUpdate.isNotEmpty()) {
            dao.upsertAll(toUpdate)
        }
    }

    /**
     * Saves a lesson plan locally first (so offline access & edits never get lost),
     * and immediately syncs to Firestore if online. If offline or remote fails transiently,
     * queues the plan with PENDING_CREATE / PENDING_UPDATE for automatic sync later.
     */
    suspend fun savePlanOfflineFirst(
        plan: LessonPlan,
        currentUserId: String,
        authorName: String
    ): Result<SaveOfflineResult> = runCatching {
        val now = System.currentTimeMillis()
        val isNewBlank = plan.id.isBlank()
        val isOwnedByAnotherUser = !isNewBlank && plan.userId.isNotBlank() && plan.userId != currentUserId

        val assignedId = if (isNewBlank || isOwnedByAnotherUser) {
            // Deterministic Firestore-compatible offline document ID
            "offline_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        } else {
            plan.id
        }

        val safeTitle = plan.title.ifBlank { plan.topic }.ifBlank { "Untitled Lesson Plan" }.take(200)
        val safeTopic = plan.topic.ifBlank { safeTitle }.take(200)
        val normalizedPlan = plan.copy(
            id = assignedId,
            userId = currentUserId,
            authorName = plan.authorName.ifBlank { authorName },
            title = safeTitle,
            topic = safeTopic
        )

        val existing = if (isNewBlank || isOwnedByAnotherUser) null else dao.getById(assignedId)
        val pendingStatus = when {
            isNewBlank || isOwnedByAnotherUser -> SyncStatus.PENDING_CREATE
            existing?.syncStatus == SyncStatus.PENDING_CREATE.name -> SyncStatus.PENDING_CREATE
            else -> SyncStatus.PENDING_UPDATE
        }

        // Persist immediately to Room so edits are durable offline
        val localEntity = normalizedPlan.toOfflineEntity(
            isDownloaded = true,
            syncStatus = pendingStatus,
            lastModifiedLocalMillis = now,
            lastSyncedMillis = existing?.lastSyncedMillis ?: 0L,
            lastSyncError = null
        )
        dao.upsert(localEntity)

        if (!connectivity.isCurrentlyOnline()) {
            return@runCatching SaveOfflineResult(
                plan = normalizedPlan,
                syncedImmediately = false,
                forkedFromAnotherAuthor = isOwnedByAnotherUser
            )
        }

        // Attempt immediate remote sync while online
        val remoteRes = if (pendingStatus == SyncStatus.PENDING_CREATE) {
            remoteRepo.createLessonPlan(normalizedPlan)
        } else {
            val updateRes = remoteRepo.updateLessonPlan(normalizedPlan)
            if (updateRes.isFailure && isNotFoundError(updateRes.exceptionOrNull())) {
                remoteRepo.createLessonPlan(normalizedPlan)
            } else {
                updateRes.map { normalizedPlan.id }
            }
        }

        return@runCatching if (remoteRes.isSuccess) {
            dao.upsert(
                localEntity.copy(
                    syncStatus = SyncStatus.SYNCED.name,
                    lastSyncedMillis = System.currentTimeMillis(),
                    lastSyncError = null
                )
            )
            SaveOfflineResult(
                plan = normalizedPlan,
                syncedImmediately = true,
                forkedFromAnotherAuthor = isOwnedByAnotherUser
            )
        } else {
            val err = remoteRes.exceptionOrNull()
            if (err is CancellationException) throw err
            Log.w(TAG, "Online save deferred to offline sync queue for plan=$assignedId", err)
            SaveOfflineResult(
                plan = normalizedPlan,
                syncedImmediately = false,
                forkedFromAnotherAuthor = isOwnedByAnotherUser
            )
        }
    }

    /**
     * Deletes an owned lesson plan offline-first.
     * If offline, marks PENDING_DELETE in Room so it hides immediately and deletes on Firestore when online.
     */
    suspend fun deletePlanOfflineFirst(planId: String, currentUserId: String): Result<Boolean> = runCatching {
        val local = dao.getById(planId)
        if (local != null && local.syncStatus == SyncStatus.PENDING_CREATE.name) {
            // Never synced to Firestore yet; safe to remove locally only
            dao.deleteById(planId)
            return@runCatching true
        }

        if (!connectivity.isCurrentlyOnline()) {
            if (local != null && local.userId == currentUserId) {
                dao.upsert(
                    local.copy(
                        syncStatus = SyncStatus.PENDING_DELETE.name,
                        lastModifiedLocalMillis = System.currentTimeMillis()
                    )
                )
            } else {
                dao.upsert(
                    LessonPlan(id = planId, userId = currentUserId).toOfflineEntity(
                        isDownloaded = false,
                        syncStatus = SyncStatus.PENDING_DELETE
                    )
                )
            }
            return@runCatching false
        }

        val remoteRes = remoteRepo.deleteLessonPlan(planId)
        if (remoteRes.isSuccess || isNotFoundError(remoteRes.exceptionOrNull())) {
            dao.deleteById(planId)
            true
        } else {
            val err = remoteRes.exceptionOrNull()
            if (err is CancellationException) throw err
            if (local != null) {
                dao.upsert(
                    local.copy(
                        syncStatus = SyncStatus.PENDING_DELETE.name,
                        lastModifiedLocalMillis = System.currentTimeMillis(),
                        lastSyncError = err?.localizedMessage
                    )
                )
            }
            false
        }
    }

    /**
     * Pushes all queued offline creations, edits, and deletions for [userId] to Firestore.
     * Guarded by a Mutex so automatic reconnect events and manual taps never conflict.
     */
    suspend fun syncPendingPlans(userId: String): SyncSummary = syncMutex.withLock {
        if (!connectivity.isCurrentlyOnline()) {
            val remaining = dao.getPendingSyncPlans(userId).size
            return@withLock SyncSummary(syncedCount = 0, failedCount = remaining)
        }

        val pending = dao.getPendingSyncPlans(userId)
        if (pending.isEmpty()) {
            return@withLock SyncSummary(syncedCount = 0, failedCount = 0)
        }

        var synced = 0
        var failed = 0
        for (entity in pending) {
            if (!connectivity.isCurrentlyOnline()) {
                failed += 1
                continue
            }
            when (entity.syncStatus) {
                SyncStatus.PENDING_CREATE.name -> {
                    val res = remoteRepo.createLessonPlan(entity.toDomain())
                    if (res.isSuccess) {
                        dao.upsert(
                            entity.copy(
                                syncStatus = SyncStatus.SYNCED.name,
                                lastSyncedMillis = System.currentTimeMillis(),
                                lastSyncError = null
                            )
                        )
                        synced++
                    } else {
                        val err = res.exceptionOrNull()
                        if (err is CancellationException) throw err
                        failed++
                        markRetryOrError(entity, err)
                    }
                }
                SyncStatus.PENDING_UPDATE.name, SyncStatus.SYNC_ERROR.name -> {
                    val domain = entity.toDomain()
                    val updateRes = remoteRepo.updateLessonPlan(domain)
                    val finalRes = if (updateRes.isFailure && isNotFoundError(updateRes.exceptionOrNull())) {
                        remoteRepo.createLessonPlan(domain).map { Unit }
                    } else {
                        updateRes
                    }
                    if (finalRes.isSuccess) {
                        dao.upsert(
                            entity.copy(
                                syncStatus = SyncStatus.SYNCED.name,
                                lastSyncedMillis = System.currentTimeMillis(),
                                lastSyncError = null
                            )
                        )
                        synced++
                    } else {
                        val err = finalRes.exceptionOrNull()
                        if (err is CancellationException) throw err
                        failed++
                        markRetryOrError(entity, err)
                    }
                }
                SyncStatus.PENDING_DELETE.name -> {
                    val res = remoteRepo.deleteLessonPlan(entity.id)
                    if (res.isSuccess || isNotFoundError(res.exceptionOrNull())) {
                        dao.deleteById(entity.id)
                        synced++
                    } else {
                        val err = res.exceptionOrNull()
                        if (err is CancellationException) throw err
                        failed++
                        markRetryOrError(entity, err)
                    }
                }
            }
        }
        SyncSummary(syncedCount = synced, failedCount = failed)
    }

    private suspend fun markRetryOrError(entity: OfflineLessonPlanEntity, err: Throwable?) {
        val fsCode = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()?.code
        val nextStatus = if (fsCode == FirebaseFirestoreException.Code.UNAVAILABLE ||
            fsCode == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) {
            entity.syncStatus
        } else {
            SyncStatus.SYNC_ERROR.name
        }
        dao.upsert(
            entity.copy(
                syncStatus = nextStatus,
                lastSyncError = err?.localizedMessage ?: "Sync error"
            )
        )
    }

    private fun isNotFoundError(err: Throwable?): Boolean {
        val fsCode = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()?.code
        return fsCode == FirebaseFirestoreException.Code.NOT_FOUND
    }

    private companion object {
        const val TAG = "OfflineLessonPlanRepo"
    }
}
