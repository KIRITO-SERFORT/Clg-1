package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ExamTimelineLogicTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) = ExamTimelineLogic.localMillis(y, m, d, h, min, ist)
    private val now = at(2026, 11, 9, 15, 30)   // Mon 9 Nov 2026, 15:30 IST

    @Test fun bucketsExams() {
        fun b(ms: Long) = ExamTimelineLogic.bucket(ms, now, ist)
        assertEquals(ExamBucket.PAST, b(at(2026, 11, 9, 9, 0)))         // earlier today = past
        assertEquals(ExamBucket.TODAY, b(at(2026, 11, 9, 18, 0)))
        assertEquals(ExamBucket.THIS_WEEK, b(at(2026, 11, 10, 9, 0)))   // tomorrow
        assertEquals(ExamBucket.THIS_WEEK, b(at(2026, 11, 16, 9, 0)))   // +7 days
        assertEquals(ExamBucket.LATER, b(at(2026, 11, 17, 9, 0)))       // +8 days
        assertEquals(ExamBucket.PAST, b(at(2026, 10, 1, 9, 0)))
    }

    @Test fun calendarDaysIgnoreClockTime() {
        assertEquals(1, ExamTimelineLogic.calendarDaysUntil(at(2026, 11, 10, 0, 5), at(2026, 11, 9, 23, 55), ist))
        assertEquals(0, ExamTimelineLogic.calendarDaysUntil(at(2026, 11, 9, 23, 59), at(2026, 11, 9, 0, 1), ist))
        assertEquals(-1, ExamTimelineLogic.calendarDaysUntil(at(2026, 11, 8, 23, 0), now, ist))
    }

    @Test fun calendarDaysAcrossDaylightSavingChange() {
        val ny = TimeZone.getTimeZone("America/New_York")      // clocks change on 8 Nov 2026
        val a = ExamTimelineLogic.localMillis(2026, 11, 7, 12, 0, ny)
        val b = ExamTimelineLogic.localMillis(2026, 11, 9, 12, 0, ny)
        assertEquals(2, ExamTimelineLogic.calendarDaysUntil(b, a, ny))
    }

    @Test fun countdownBreakdown() {
        val c = ExamTimelineLogic.countdown(now + (2 * 1440 + 3 * 60 + 5) * 60_000L, now)
        assertEquals(2L, c.days); assertEquals(3L, c.hours); assertEquals(5L, c.minutes)
        assertEquals("2d 3h", ExamTimelineLogic.countdownText(c))
        assertEquals("1h 30m", ExamTimelineLogic.countdownText(ExamTimelineLogic.countdown(now + 90 * 60_000L, now)))
        assertEquals("45m", ExamTimelineLogic.countdownText(ExamTimelineLogic.countdown(now + 45 * 60_000L, now)))
        assertEquals("Started", ExamTimelineLogic.countdownText(ExamTimelineLogic.countdown(now - 1, now)))
    }

    @Test fun relativeLabels() {
        assertEquals("Today", ExamTimelineLogic.relativeLabel(0)); assertEquals("Tomorrow", ExamTimelineLogic.relativeLabel(1))
        assertEquals("In 5 days", ExamTimelineLogic.relativeLabel(5)); assertEquals("Yesterday", ExamTimelineLogic.relativeLabel(-1))
        assertEquals("3 days ago", ExamTimelineLogic.relativeLabel(-3))
    }

    @Test fun datePickerUsesUtcDay() {
        // The picker hands back UTC midnight. Reading it in IST (UTC+5:30) would still give the same day here, but
        // in a negative-offset zone it would give the PREVIOUS day: we must read it in UTC.
        val picked = ExamTimelineLogic.ymdToPickerMillis(2026, 12, 5)
        assertEquals(Triple(2026, 12, 5), ExamTimelineLogic.pickerMillisToYmd(picked))
        val la = TimeZone.getTimeZone("America/Los_Angeles")
        val wrong = ExamTimelineLogic.localYmdhm(picked, la)
        assertEquals(4, wrong[2])                              // proves the trap exists; our function avoids it
    }

    @Test fun localMillisRoundTrip() {
        val ms = ExamTimelineLogic.localMillis(2026, 12, 5, 9, 30, ist)
        assertEquals(listOf(2026, 12, 5, 9, 30), ExamTimelineLogic.localYmdhm(ms, ist).toList())
    }

    @Test fun pastRemindersAreDropped() {
        val exam = now + 5 * 3_600_000L                         // exam in 5 hours
        val day = 86_400_000L; val twoH = 7_200_000L
        val r = ExamTimelineLogic.upcomingReminders(exam, now, listOf(day, twoH))
        assertEquals(listOf(exam - twoH), r)                   // the 1-day reminder is already in the past: skipped
        assertTrue(ExamTimelineLogic.upcomingReminders(now - 1, now, listOf(day, twoH)).isEmpty())
    }
}
