package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_plan_progress WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeProgressForUser(userId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_plan_progress WHERE lessonPlanId = :planId LIMIT 1")
    suspend fun getByPlanId(planId: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_plan_progress WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getPendingForUser(userId: String): List<LessonProgressEntity>

    @Query("SELECT COUNT(*) FROM lesson_plan_progress WHERE userId = :userId AND syncStatus != 'SYNCED'")
    fun observePendingCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LessonProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LessonProgressEntity>)
}
