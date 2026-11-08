package com.example.widget

/** Decides when widget data should be flagged as old. Pure logic, no Android dependencies. */
object WidgetFreshness {
    /** Sync runs every few hours by default; flag the data as old after 12 hours without a successful sync. */
    const val STALE_AFTER_MILLIS: Long = 12L * 60 * 60 * 1000

    fun isStale(lastSyncedMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        lastSyncedMillis <= 0L || nowMillis - lastSyncedMillis > STALE_AFTER_MILLIS
}
