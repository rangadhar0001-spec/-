package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OfflineLessonPlanEntity::class, LessonProgressEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineLessonPlanDao(): OfflineLessonPlanDao
    abstract fun lessonProgressDao(): LessonProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lesson_plan_progress` (
                        `lessonPlanId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `stage` TEXT NOT NULL,
                        `aditiCompleted` INTEGER NOT NULL,
                        `bodhaCompleted` INTEGER NOT NULL,
                        `abhyasaCompleted` INTEGER NOT NULL,
                        `prayogaCompleted` INTEGER NOT NULL,
                        `prasaraCompleted` INTEGER NOT NULL,
                        `checkedGuidelineIdsCsv` TEXT NOT NULL,
                        `achievedOutcomeIndicesCsv` TEXT NOT NULL,
                        `inProgressOutcomeIndicesCsv` TEXT NOT NULL,
                        `inclusiveSupportApplied` INTEGER NOT NULL,
                        `tlmUsedInClass` INTEGER NOT NULL,
                        `assessmentConducted` INTEGER NOT NULL,
                        `classroomNotes` TEXT NOT NULL,
                        `lastTrackedDay` INTEGER NOT NULL,
                        `lastTrackedMonth` INTEGER NOT NULL,
                        `lastTrackedYear` INTEGER NOT NULL,
                        `completionPercent` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`lessonPlanId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "odia_sikshya_sathi.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
