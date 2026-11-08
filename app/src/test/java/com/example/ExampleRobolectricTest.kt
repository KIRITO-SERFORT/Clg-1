package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val widgetName = context.getString(R.string.widget_name)
        val compactWidgetName = context.getString(R.string.compact_widget_name)
        assertEquals("Clg 1%", appName)
        assertEquals("Clg 1%", widgetName)
        assertEquals("Clg 1% Pie Widget", compactWidgetName)
    }

    @Test
    fun `attendance summary json serialization works in android environment`() {
        val original = AttendanceSummary(
            present = 150,
            absent = 13,
            marked = 163,
            notMarked = 127,
            totalWorking = 290,
            apiPercentage = 92,
            tillDate = "06-11-2026",
            lastSyncedMillis = 1730880000000L
        )

        val jsonStr = original.toJson()
        val parsed = AttendanceSummary.fromJson(jsonStr)

        assertNotNull(parsed)
        assertEquals(original.present, parsed?.present)
        assertEquals(original.absent, parsed?.absent)
        assertEquals(original.marked, parsed?.marked)
        assertEquals(original.notMarked, parsed?.notMarked)
        assertEquals(original.totalWorking, parsed?.totalWorking)
        assertEquals(original.apiPercentage, parsed?.apiPercentage)
        assertEquals(original.tillDate, parsed?.tillDate)
        assertEquals(original.lastSyncedMillis, parsed?.lastSyncedMillis)
    }

    @Test
    fun `subject attendance list serialization works in android environment`() {
        val list = listOf(
            SubjectAttendance(
                code = "26EGHS11",
                title = "PROFESSIONAL ENGLISH -I",
                facultyName = "AMEERA NASREEN",
                totalHours = 30,
                presentHours = 26,
                absentHours = 4,
                openHours = 11,
                totalWorking = 41,
                percentage = 86.67
            ),
            SubjectAttendance(
                code = "26PHBS1B",
                title = "PHYSICS FOR MECHANICAL ENGINEERING-I",
                facultyName = "SIVAGANESH G",
                totalHours = 36,
                presentHours = 36,
                absentHours = 0,
                openHours = 6,
                totalWorking = 42,
                percentage = 100.0
            )
        )

        val jsonStr = SubjectAttendance.listToJson(list)
        val parsed = SubjectAttendance.listFromJson(jsonStr)

        assertEquals(2, parsed.size)
        assertEquals("26EGHS11", parsed[0].code)
        assertEquals("PROFESSIONAL ENGLISH -I", parsed[0].title)
        assertEquals("AMEERA NASREEN", parsed[0].facultyName)
        assertEquals(26, parsed[0].presentHours)
        assertEquals(86.67, parsed[0].percentage, 0.01)

        assertEquals("26PHBS1B", parsed[1].code)
        assertEquals(36, parsed[1].presentHours)
        assertEquals(100.0, parsed[1].percentage, 0.01)
    }
}
