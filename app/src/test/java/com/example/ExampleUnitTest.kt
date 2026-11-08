package com.example

import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAttendanceCalculations_fromSyncedData() {
        val summary = AttendanceSummary(
            present = 150,
            absent = 13,
            marked = 163,
            notMarked = 127,
            totalWorking = 290,
            apiPercentage = 92,
            tillDate = "2026-11-06"
        )

        assertEquals(92.02, summary.percentage, 0.01)
        assertEquals("92.0%", summary.formattedPercentage)
        assertEquals(37, summary.safeToMiss(0.75))
        assertEquals(0, summary.hoursToRecover(0.75))
        assertEquals(13, summary.safeToMiss(0.85))
    }

    @Test
    fun testRecoveryCalculation_whenShortage() {
        val shortage = AttendanceSummary(
            present = 60,
            absent = 40,
            marked = 100,
            notMarked = 50,
            totalWorking = 150,
            apiPercentage = 60,
            tillDate = "2026-11-06"
        )

        assertEquals(60.0, shortage.percentage, 0.001)
        assertEquals(0, shortage.safeToMiss(0.75))
        assertEquals(60, shortage.hoursToRecover(0.75))
    }

    @Test
    fun testSubjectAttendanceCalculations() {
        // Professional English: 26 present, 4 absent, 30 total (86.67%)
        val english = SubjectAttendance(
            code = "26EGHS11",
            title = "PROFESSIONAL ENGLISH -I",
            facultyName = "AMEERA NASREEN",
            totalHours = 30,
            presentHours = 26,
            absentHours = 4,
            openHours = 11,
            totalWorking = 41,
            percentage = 86.67
        )

        assertFalse(english.isBelowExamThreshold(0.75))
        // Safe to miss at 75%: floor(26 / 0.75 - 30) = floor(34.66 - 30) = 4 classes
        assertEquals(4, english.safeToMiss(0.75))
        assertEquals(0, english.hoursToRecover(0.75))

        // Shortage subject: e.g. 10 present out of 20 total = 50%
        val shortageSubject = SubjectAttendance(
            code = "26TEST01",
            title = "TEST LAB",
            facultyName = "FACULTY",
            totalHours = 20,
            presentHours = 10,
            absentHours = 10,
            openHours = 5,
            totalWorking = 25,
            percentage = 50.0
        )

        assertTrue(shortageSubject.isBelowExamThreshold(0.75))
        assertEquals(0, shortageSubject.safeToMiss(0.75))
        // Hours to recover: ceil((0.75 * 20 - 10) / (1 - 0.75)) = ceil((15 - 10) / 0.25) = 20
        assertEquals(20, shortageSubject.hoursToRecover(0.75))
    }
}
