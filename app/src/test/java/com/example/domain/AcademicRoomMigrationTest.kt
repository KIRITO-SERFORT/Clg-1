package com.example.domain

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AcademicRoomRepository
import com.example.data.local.Clg1Database
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicRoomMigrationTest {
    @Test fun importsLegacyAcademicPreferencesIntoRoomWithoutDeletingTheLegacyCopy() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("clg1_academic.db")
        context.getSharedPreferences("clg1_academic_local", Context.MODE_PRIVATE).edit()
            .putString("college", "Example Institute")
            .putString("term", "Semester 1")
            .putString("academic_records_v1", """{"schema_version":1,"subjects":[{"id":"s1","code":"CS101","name":"Computing","credits":3,"assessments":[{"name":"Internal","obtained":18,"maximum":20,"weight":40}]}],"exams":[]}""")
            .apply()

        AcademicRoomRepository.migrateLegacyPreferences(context)
        val dao = Clg1Database.get(context).academicDao()
        assertEquals("Example Institute", dao.getProfile()?.college)
        assertEquals(1, dao.getSubjects().size)
        assertEquals(1, dao.getAssessmentMarks().size)
        assertNotNull(context.getSharedPreferences("clg1_academic_local", Context.MODE_PRIVATE).getString("academic_records_v1", null))
    }
}
