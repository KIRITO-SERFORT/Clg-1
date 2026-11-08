package com.example

import com.example.util.DayOrderAnchor
import com.example.util.DayOrderCalculator
import com.example.util.SimpleDate
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Expected values are worked out by hand, NOT produced by the calculator.
 * Reference: 2026-11-06 is a Friday and is "Day 3" in a 5-day-order timetable.
 */
class DayOrderCalculatorTest {

    private val anchor = DayOrderAnchor(SimpleDate(2026, 11, 6), 3)
    private fun d(day: Int) = SimpleDate(2026, 11, day)

    private val monToSat = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    @Test fun anchorDayKeepsItsOwnDayOrder() = assertEquals(3, DayOrderCalculator.dayOrderFor(d(6), anchor, 5))

    @Test fun weekendRollsForwardToMonday() {
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(7), anchor, 5))
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(8), anchor, 5))
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(9), anchor, 5))
    }

    @Test fun cycleWrapsAround() {
        assertEquals(5, DayOrderCalculator.dayOrderFor(d(10), anchor, 5))
        assertEquals(1, DayOrderCalculator.dayOrderFor(d(11), anchor, 5))
    }

    @Test fun datesBeforeTheAnchorCountBackwards() {
        assertEquals(2, DayOrderCalculator.dayOrderFor(d(5), anchor, 5))
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(2), anchor, 5))
    }

    @Test fun sixWorkingDaysPerWeek() {
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(7), anchor, 6, monToSat))
        assertEquals(5, DayOrderCalculator.dayOrderFor(d(8), anchor, 6, monToSat))
        assertEquals(5, DayOrderCalculator.dayOrderFor(d(16), anchor, 6, monToSat))
        assertFalse(DayOrderCalculator.isWeekend(d(7), monToSat))
        assertTrue(DayOrderCalculator.isWeekend(d(8), monToSat))
    }

    @Test fun emptyWorkingDaysFallBackToMondayToFriday() =
        assertEquals(4, DayOrderCalculator.dayOrderFor(d(7), anchor, 5, emptySet()))

    @Test fun zeroDayOrdersIsTreatedAsOne() = assertEquals(1, DayOrderCalculator.dayOrderFor(d(10), anchor, 0))
}
