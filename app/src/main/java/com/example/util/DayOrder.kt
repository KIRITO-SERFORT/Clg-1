package com.example.util

import java.util.Calendar
import java.util.Locale

/** A calendar date without a time of day. Month is 1-12. */
data class SimpleDate(val year: Int, val month: Int, val day: Int) {
    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }

    fun serialize(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    companion object {
        fun from(c: Calendar) = SimpleDate(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )

        fun today(): SimpleDate = from(Calendar.getInstance())

        fun parse(text: String): SimpleDate? {
            val p = text.trim().split("-")
            if (p.size != 3) return null
            val y = p[0].toIntOrNull() ?: return null
            val m = p[1].toIntOrNull() ?: return null
            val d = p[2].toIntOrNull() ?: return null
            return SimpleDate(y, m, d)
        }
    }
}

/** "On [date] the college ran day order [dayOrder]". Everything else is counted from here. */
data class DayOrderAnchor(val date: SimpleDate, val dayOrder: Int) {
    fun serialize(): String = "${date.serialize()}|$dayOrder"

    companion object {
        fun parse(text: String): DayOrderAnchor? {
            val parts = text.split("|")
            if (parts.size != 2) return null
            val date = SimpleDate.parse(parts[0]) ?: return null
            val order = parts[1].toIntOrNull() ?: return null
            return DayOrderAnchor(date, order)
        }
    }
}

/**
 * The college runs a rotating "day order" (Day 1..Day N). By default only Monday to Friday count as
 * teaching days; pass a different [workingDays] set (Calendar.MONDAY..Calendar.SUNDAY values) for colleges
 * with a Saturday session or another week pattern. Every function defaults to Mon-Fri, so existing callers
 * behave exactly as before.
 *
 * The portal does not publish today's day order, so we remember one known day (the anchor) and count
 * teaching days from it. Days that are not teaching days are skipped.
 */
object DayOrderCalculator {

    val DEFAULT_WORKING_DAYS: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )

    /** An empty set would loop forever, so it falls back to the default week. */
    private fun safe(days: Set<Int>): Set<Int> = if (days.isEmpty()) DEFAULT_WORKING_DAYS else days

    private fun isOffDay(c: Calendar, workingDays: Set<Int>): Boolean =
        c.get(Calendar.DAY_OF_WEEK) !in safe(workingDays)

    fun isWeekend(date: SimpleDate, workingDays: Set<Int> = DEFAULT_WORKING_DAYS): Boolean =
        isOffDay(date.toCalendar(), workingDays)

    /** A non-teaching day rolls forward to the next teaching day. */
    fun nextWorkingDay(from: SimpleDate, workingDays: Set<Int> = DEFAULT_WORKING_DAYS): SimpleDate {
        val c = from.toCalendar()
        while (isOffDay(c, workingDays)) c.add(Calendar.DAY_OF_MONTH, 1)
        return SimpleDate.from(c)
    }

    /** Teaching days between [anchor] and [target]; negative when target is earlier. */
    private fun workingDaysBetween(anchor: SimpleDate, target: SimpleDate, workingDays: Set<Int>): Int {
        val a = anchor.toCalendar()
        val t = target.toCalendar()
        var count = 0
        if (!a.after(t)) {
            while (a.before(t)) {
                a.add(Calendar.DAY_OF_MONTH, 1)
                if (!isOffDay(a, workingDays)) count++
            }
        } else {
            while (a.after(t)) {
                if (!isOffDay(a, workingDays)) count--
                a.add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        return count
    }

    fun dayOrderFor(
        date: SimpleDate,
        anchor: DayOrderAnchor,
        noOfDays: Int,
        workingDays: Set<Int> = DEFAULT_WORKING_DAYS
    ): Int {
        val days = noOfDays.coerceAtLeast(1)
        val working = nextWorkingDay(date, workingDays)
        val n = workingDaysBetween(anchor.date, working, workingDays)
        val index = (((anchor.dayOrder - 1 + n) % days) + days) % days
        return index + 1
    }

    fun todayDayOrder(anchor: DayOrderAnchor, noOfDays: Int, workingDays: Set<Int> = DEFAULT_WORKING_DAYS): Int =
        dayOrderFor(SimpleDate.today(), anchor, noOfDays, workingDays)

    /** The user says "today is Day N". On a non-teaching day that means the next teaching day. */
    fun anchorForToday(dayOrder: Int, workingDays: Set<Int> = DEFAULT_WORKING_DAYS): DayOrderAnchor =
        DayOrderAnchor(nextWorkingDay(SimpleDate.today(), workingDays), dayOrder)

    fun isWeekendToday(workingDays: Set<Int> = DEFAULT_WORKING_DAYS): Boolean =
        isWeekend(SimpleDate.today(), workingDays)

    /** Maps the device's current day of the week to Day 1..noOfDays (Mon=1, Tue=2, ..., Fri=5). */
    fun deviceWeekdayDayOrder(noOfDays: Int): Int {
        val days = noOfDays.coerceAtLeast(1)
        val cal = Calendar.getInstance()
        val rawDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> if (days >= 6) 6 else 1
            else -> 1 // Sunday -> Monday (Day 1)
        }
        return ((rawDay - 1) % days) + 1
    }
}
