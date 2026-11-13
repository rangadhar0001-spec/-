package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineLessonPlanDao {

    @Query(
        """
        SELECT * FROM offline_lesson_plans
        WHERE isDownloaded = 1 AND syncStatus != 'PENDING_DELETE'
        ORDER BY lastModifiedLocalMillis DESC
        """
    )
    fun observeDownloadedPlans(): Flow<List<OfflineLessonPlanEntity>>

    @Query(
        """
        SELECT * FROM offline_lesson_plans
        WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'
        ORDER BY lastModifiedLocalMillis DESC
        """
    )
    fun observeUserLocalPlans(userId: String): Flow<List<OfflineLessonPlanEntity>>

    @Query(
        """
        SELECT * FROM offline_lesson_plans
        WHERE isDownloaded = 1 AND status = 'published' AND syncStatus != 'PENDING_DELETE'
        ORDER BY lastModifiedLocalMillis DESC
        """
    )
    fun observeDownloadedPublishedPlans(): Flow<List<OfflineLessonPlanEntity>>

    @Query(
        """
        SELECT id FROM offline_lesson_plans
        WHERE isDownloaded = 1 AND syncStatus != 'PENDING_DELETE'
        """
    )
    fun observeDownloadedPlanIds(): Flow<List<String>>

    @Query(
        """
        SELECT id FROM offline_lesson_plans
        WHERE userId = :userId AND syncStatus IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'SYNC_ERROR')
        """
    )
    fun observePendingSyncIds(userId: String): Flow<List<String>>

    @Query(
        """
        SELECT COUNT(*) FROM offline_lesson_plans
        WHERE userId = :userId AND syncStatus IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'SYNC_ERROR')
        """
    )
    fun observePendingSyncCount(userId: String): Flow<Int>

    @Query("SELECT * FROM offline_lesson_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OfflineLessonPlanEntity?

    @Query(
        """
        SELECT * FROM offline_lesson_plans
        WHERE userId = :userId AND syncStatus IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'SYNC_ERROR')
        ORDER BY lastModifiedLocalMillis ASC
        """
    )
    suspend fun getPendingSyncPlans(userId: String): List<OfflineLessonPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineLessonPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OfflineLessonPlanEntity>)

    @Query("DELETE FROM offline_lesson_plans WHERE id = :id")
    suspend fun deleteById(id: String)
}
