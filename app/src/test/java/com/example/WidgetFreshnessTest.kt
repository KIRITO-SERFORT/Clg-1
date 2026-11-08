package com.example

import com.example.widget.WidgetFreshness
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFreshnessTest {
    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000

    @Test fun neverSyncedIsStale() = assertTrue(WidgetFreshness.isStale(0L, now))
    @Test fun recentDataIsFresh() = assertFalse(WidgetFreshness.isStale(now - hour, now))
    @Test fun exactlyTwelveHoursIsStillFresh() = assertFalse(WidgetFreshness.isStale(now - 12 * hour, now))
    @Test fun moreThanTwelveHoursIsStale() = assertTrue(WidgetFreshness.isStale(now - 12 * hour - 1, now))
}
