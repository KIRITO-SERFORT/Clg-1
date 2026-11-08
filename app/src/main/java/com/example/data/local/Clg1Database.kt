package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AcademicProfileEntity::class,
        AcademicPeriodEntity::class,
        AcademicSubjectEntity::class,
        AssessmentMarkEntity::class,
        AcademicExamEntity::class,
        AcademicSnapshotEntity::class,
        AcademicAuditEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class Clg1Database : RoomDatabase() {
    abstract fun academicDao(): AcademicDao

    companion object {
        @Volatile private var INSTANCE: Clg1Database? = null

        // Version 1 is the initial structured workspace. Migration 1->2 adds historical snapshots.
        // Migration 2->3 adds an audit log; both preserve every existing row.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `academic_term_snapshots` (`id` TEXT NOT NULL, `academicYear` TEXT NOT NULL, `termName` TEXT NOT NULL, `capturedAtMillis` INTEGER NOT NULL, `subjectCount` INTEGER NOT NULL, `completedSubjectCount` INTEGER NOT NULL, `averagePercentage` REAL, `sgpa` REAL, `credits` REAL NOT NULL, `subjectSummariesJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_term_snapshots_capturedAtMillis` ON `academic_term_snapshots` (`capturedAtMillis`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `academic_audit` (`id` TEXT NOT NULL, `recordId` TEXT NOT NULL, `action` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `detail` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_audit_createdAtMillis` ON `academic_audit` (`createdAtMillis`)")
            }
        }

        fun get(context: Context): Clg1Database = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                Clg1Database::class.java,
                "clg1_academic.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Intentionally no fallbackToDestructiveMigration: an unknown path must not erase records.
                .build().also { INSTANCE = it }
        }
    }
}
