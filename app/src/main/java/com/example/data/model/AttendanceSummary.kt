package com.example.data.model

import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.floor

data class AttendanceSummary(
    val present: Int,
    val absent: Int,
    val marked: Int,
    val notMarked: Int,
    val totalWorking: Int,
    val apiPercentage: Int,
    val tillDate: String,
    val lastSyncedMillis: Long = System.currentTimeMillis()
) {
    /** Exact percentage calculated on marked hours (matches college portal: present / marked). */
    val percentage: Double
        get() = if (marked == 0) 0.0 else (present * 100.0) / marked

    /** Formatted percentage string (e.g. "92.0%"). */
    val formattedPercentage: String
        get() = String.format("%.1f%%", percentage)

    /** Hours you can safely miss while remaining at or above [threshold] (0.0 to 1.0, e.g. 0.75). */
    fun safeToMiss(threshold: Double = 0.75): Int {
        if (marked == 0) return 0
        val safe = floor(present / threshold - marked + 1e-9).toInt()
        return safe.coerceAtLeast(0)
    }

    /** Consecutive hours you must attend to reach [threshold]. Returns 0 if already above. */
    fun hoursToRecover(threshold: Double = 0.75): Int {
        if (marked == 0 || present.toDouble() / marked >= threshold) return 0
        return ceil((threshold * marked - present) / (1.0 - threshold) - 1e-9).toInt().coerceAtLeast(0)
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("present", present)
        json.put("absent", absent)
        json.put("marked", marked)
        json.put("notMarked", notMarked)
        json.put("totalWorking", totalWorking)
        json.put("apiPercentage", apiPercentage)
        json.put("tillDate", tillDate)
        json.put("lastSyncedMillis", lastSyncedMillis)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AttendanceSummary? {
            return try {
                val j = JSONObject(jsonStr)
                AttendanceSummary(
                    present = j.getInt("present"),
                    absent = j.getInt("absent"),
                    marked = j.getInt("marked"),
                    notMarked = j.getInt("notMarked"),
                    totalWorking = j.getInt("totalWorking"),
                    apiPercentage = j.getInt("apiPercentage"),
                    tillDate = j.optString("tillDate", ""),
                    lastSyncedMillis = j.optLong("lastSyncedMillis", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
