package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.LessonPlanProgress
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "lesson_plan_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonPlanId: String,
    val userId: String,
    val stage: String,
    val aditiCompleted: Boolean,
    val bodhaCompleted: Boolean,
    val abhyasaCompleted: Boolean,
    val prayogaCompleted: Boolean,
    val prasaraCompleted: Boolean,
    val checkedGuidelineIdsCsv: String,
    val achievedOutcomeIndicesCsv: String,
    val inProgressOutcomeIndicesCsv: String,
    val inclusiveSupportApplied: Boolean,
    val tlmUsedInClass: Boolean,
    val assessmentConducted: Boolean,
    val classroomNotes: String,
    val lastTrackedDay: Long,
    val lastTrackedMonth: Long,
    val lastTrackedYear: Long,
    val completionPercent: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.SYNCED.name
) {
    fun toDomain(): LessonPlanProgress = LessonPlanProgress(
        id = lessonPlanId,
        userId = userId,
        lessonPlanId = lessonPlanId,
        stage = stage,
        aditiCompleted = aditiCompleted,
        bodhaCompleted = bodhaCompleted,
        abhyasaCompleted = abhyasaCompleted,
        prayogaCompleted = prayogaCompleted,
        prasaraCompleted = prasaraCompleted,
        checkedGuidelineIds = checkedGuidelineIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        achievedOutcomeIndices = achievedOutcomeIndicesCsv.split(",").mapNotNull { it.trim().toLongOrNull() },
        inProgressOutcomeIndices = inProgressOutcomeIndicesCsv.split(",").mapNotNull { it.trim().toLongOrNull() },
        inclusiveSupportApplied = inclusiveSupportApplied,
        tlmUsedInClass = tlmUsedInClass,
        assessmentConducted = assessmentConducted,
        classroomNotes = classroomNotes,
        lastTrackedDay = lastTrackedDay,
        lastTrackedMonth = lastTrackedMonth,
        lastTrackedYear = lastTrackedYear,
        completionPercent = completionPercent,
        updatedAt = if (updatedAtMillis > 0L) Timestamp(Date(updatedAtMillis)) else null
    )
}

fun LessonPlanProgress.toEntity(
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    updatedAtMillis: Long = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
): LessonProgressEntity = LessonProgressEntity(
    lessonPlanId = lessonPlanId.ifBlank { id },
    userId = userId,
    stage = stage,
    aditiCompleted = aditiCompleted,
    bodhaCompleted = bodhaCompleted,
    abhyasaCompleted = abhyasaCompleted,
    prayogaCompleted = prayogaCompleted,
    prasaraCompleted = prasaraCompleted,
    checkedGuidelineIdsCsv = checkedGuidelineIds.joinToString(","),
    achievedOutcomeIndicesCsv = achievedOutcomeIndices.joinToString(","),
    inProgressOutcomeIndicesCsv = inProgressOutcomeIndices.joinToString(","),
    inclusiveSupportApplied = inclusiveSupportApplied,
    tlmUsedInClass = tlmUsedInClass,
    assessmentConducted = assessmentConducted,
    classroomNotes = classroomNotes,
    lastTrackedDay = lastTrackedDay,
    lastTrackedMonth = lastTrackedMonth,
    lastTrackedYear = lastTrackedYear,
    completionPercent = completionPercent,
    updatedAtMillis = updatedAtMillis,
    syncStatus = syncStatus.name
)
