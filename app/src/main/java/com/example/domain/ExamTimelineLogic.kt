package com.example.domain

import java.util.Calendar
import java.util.TimeZone

enum class ExamBucket { TODAY, THIS_WEEK, LATER, PAST }

data class ExamCountdown(val days: Long, val hours: Long, val minutes: Long, val started: Boolean)

/** Pure calendar maths for the exam timeline. Everything takes the time zone explicitly so it is testable. */
object ExamTimelineLogic {

    private fun midnight(millis: Long, tz: TimeZone): Long {
        val c = Calendar.getInstance(tz).apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Calendar days from today to the exam's day (0 = same day, negative = earlier). DST-safe because it rounds. */
    fun calendarDaysUntil(examMillis: Long, nowMillis: Long, tz: TimeZone): Int =
        Math.round((midnight(examMillis, tz) - midnight(nowMillis, tz)) / 86_400_000.0).toInt()

    fun bucket(examMillis: Long, nowMillis: Long, tz: TimeZone): ExamBucket {
        if (examMillis < nowMillis) return ExamBucket.PAST
        return when (calendarDaysUntil(examMillis, nowMillis, tz)) {
            0 -> ExamBucket.TODAY
            in 1..7 -> ExamBucket.THIS_WEEK
            else -> ExamBucket.LATER
        }
    }

    fun countdown(examMillis: Long, nowMillis: Long): ExamCountdown {
        val diff = examMillis - nowMillis
        if (diff <= 0) return ExamCountdown(0, 0, 0, started = true)
        val totalMinutes = diff / 60_000L
        return ExamCountdown(totalMinutes / 1440, (totalMinutes % 1440) / 60, totalMinutes % 60, started = false)
    }

    fun countdownText(c: ExamCountdown): String = when {
        c.started -> "Started"
        c.days >= 1 -> "${c.days}d ${c.hours}h"
        c.hours >= 1 -> "${c.hours}h ${c.minutes}m"
        else -> "${c.minutes}m"
    }

    fun relativeLabel(days: Int): String = when {
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        days > 1 -> "In $days days"
        days == -1 -> "Yesterday"
        else -> "${-days} days ago"
    }

    /** Material date pickers return UTC midnight of the chosen day. Read the date in UTC, not in the phone's zone. */
    fun pickerMillisToYmd(pickerUtcMillis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerUtcMillis }
        return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun ymdToPickerMillis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y, m - 1, d) }
        return c.timeInMillis
    }

    fun localMillis(y: Int, m: Int, d: Int, hour: Int, minute: Int, tz: TimeZone): Long {
        val c = Calendar.getInstance(tz).apply { clear(); isLenient = false; set(y, m - 1, d, hour, minute, 0) }
        return c.timeInMillis
    }

    fun localYmdhm(millis: Long, tz: TimeZone): IntArray {
        val c = Calendar.getInstance(tz).apply { timeInMillis = millis }
        return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    /** Reminder moments (millis) that are still in the future. Past ones are skipped: no late "instant" notifications. */
    fun upcomingReminders(examMillis: Long, nowMillis: Long, leadMillis: List<Long>): List<Long> =
        leadMillis.map { examMillis - it }.filter { it > nowMillis }
}
