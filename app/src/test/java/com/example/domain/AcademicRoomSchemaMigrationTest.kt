package com.example.domain

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Clg1Database
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicRoomSchemaMigrationTest {
    @Test fun versionOneDatabaseMigratesToVersionThreeWithoutLosingWorkspaceRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "clg1-migration-fixture.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            sqlite.execSQL("CREATE TABLE academic_profile (id INTEGER NOT NULL PRIMARY KEY, college TEXT NOT NULL, termName TEXT NOT NULL, academicYear TEXT NOT NULL, configurationJson TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)")
            sqlite.execSQL("CREATE TABLE academic_periods (id TEXT NOT NULL PRIMARY KEY, profileId INTEGER NOT NULL, name TEXT NOT NULL, orderIndex INTEGER NOT NULL, startsAtMillis INTEGER, endsAtMillis INTEGER, examAtMillis INTEGER)")
            sqlite.execSQL("CREATE INDEX index_academic_periods_profileId ON academic_periods(profileId)")
            sqlite.execSQL("CREATE TABLE academic_subjects (id TEXT NOT NULL PRIMARY KEY, code TEXT NOT NULL, name TEXT NOT NULL, credits REAL NOT NULL, termId TEXT NOT NULL, subjectPassPercent REAL)")
            sqlite.execSQL("CREATE INDEX index_academic_subjects_termId ON academic_subjects(termId)")
            sqlite.execSQL("CREATE TABLE assessment_marks (id TEXT NOT NULL PRIMARY KEY, subjectId TEXT NOT NULL, name TEXT NOT NULL, obtained REAL, maximum REAL NOT NULL, weightPercent REAL NOT NULL, status TEXT NOT NULL, minimumPassPercent REAL, category TEXT NOT NULL, dateEpochMillis INTEGER, source TEXT NOT NULL, sequence INTEGER NOT NULL)")
            sqlite.execSQL("CREATE INDEX index_assessment_marks_subjectId ON assessment_marks(subjectId)")
            sqlite.execSQL("CREATE TABLE academic_exams (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, subject TEXT NOT NULL, dateMillis INTEGER NOT NULL, notes TEXT NOT NULL, termId TEXT NOT NULL)")
            sqlite.execSQL("CREATE INDEX index_academic_exams_dateMillis ON academic_exams(dateMillis)")
            sqlite.execSQL("INSERT INTO academic_profile VALUES (1, 'Legacy College', 'Semester 1', '2026-27', '{}', 1000)")
            sqlite.version = 1
        } finally {
            sqlite.close()
        }

        val migrated = Room.databaseBuilder(context, Clg1Database::class.java, name)
            .addMigrations(Clg1Database.MIGRATION_1_2, Clg1Database.MIGRATION_2_3)
            .build()
        try {
            val profile = migrated.academicDao().getProfile()
            assertEquals("Legacy College", profile?.college)
            assertTrue(migrated.academicDao().getSnapshots().isEmpty())
            assertTrue(migrated.academicDao().getAudits().isEmpty())
            migrated.openHelper.writableDatabase.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
