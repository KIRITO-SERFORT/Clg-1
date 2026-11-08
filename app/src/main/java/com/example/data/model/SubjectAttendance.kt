package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.floor

data class SubjectAttendance(
    val code: String,
    val title: String,
    val facultyName: String,
    val totalHours: Int,       // total hours entered/marked
    val presentHours: Int,
    val absentHours: Int,
    val openHours: Int,         // hours not marked
    val totalWorking: Int,
    val percentage: Double
) {
    /** Hours student can miss in this subject and stay >= [threshold] (e.g. 0.75). */
    fun safeToMiss(threshold: Double = 0.75): Int {
        if (totalHours == 0) return 0
        val safe = floor(presentHours / threshold - totalHours + 1e-9).toInt()
        return safe.coerceAtLeast(0)
    }

    /** Consecutive hours student must attend in this subject to climb back to [threshold]. */
    fun hoursToRecover(threshold: Double = 0.75): Int {
        if (totalHours == 0 || percentage >= threshold * 100.0) return 0
        return ceil((threshold * totalHours - presentHours) / (1.0 - threshold) - 1e-9).toInt().coerceAtLeast(0)
    }

    /** Whether student's attendance in this subject is below the mandatory exam threshold. */
    fun isBelowExamThreshold(threshold: Double = 0.75): Boolean {
        return totalHours > 0 && percentage < (threshold * 100.0)
    }

    fun toJson(): JSONObject {
        val j = JSONObject()
        j.put("code", code)
        j.put("title", title)
        j.put("facultyName", facultyName)
        j.put("totalHours", totalHours)
        j.put("presentHours", presentHours)
        j.put("absentHours", absentHours)
        j.put("openHours", openHours)
        j.put("totalWorking", totalWorking)
        j.put("percentage", percentage)
        return j
    }

    companion object {
        fun fromJson(j: JSONObject): SubjectAttendance {
            return SubjectAttendance(
                code = j.optString("code", ""),
                title = j.optString("title", ""),
                facultyName = j.optString("facultyName", ""),
                totalHours = j.optInt("totalHours", 0),
                presentHours = j.optInt("presentHours", 0),
                absentHours = j.optInt("absentHours", 0),
                openHours = j.optInt("openHours", 0),
                totalWorking = j.optInt("totalWorking", 0),
                percentage = j.optDouble("percentage", 0.0)
            )
        }

        fun fromErpDetail(jDetail: JSONObject): SubjectAttendance {
            val jCourse = jDetail.optJSONObject("course") ?: JSONObject()
            val jHours = jDetail.optJSONObject("hours") ?: JSONObject()
            val jPercentage = jDetail.optJSONObject("percentage") ?: JSONObject()
            val jFacultyArr = jDetail.optJSONArray("faculty")
            val facultyName = if (jFacultyArr != null && jFacultyArr.length() > 0) {
                jFacultyArr.optJSONObject(0)?.optString("name", "").orEmpty()
            } else ""

            return SubjectAttendance(
                code = jCourse.optString("code", ""),
                title = jCourse.optString("title", ""),
                facultyName = facultyName,
                totalHours = jHours.optInt("total", 0),
                presentHours = jHours.optInt("present", 0),
                absentHours = jHours.optInt("absent", 0),
                openHours = jHours.optInt("open", 0),
                totalWorking = jHours.optInt("totalworking", 0),
                percentage = jPercentage.optDouble("present", 0.0)
            )
        }

        fun listToJson(list: List<SubjectAttendance>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJson(jsonStr: String): List<SubjectAttendance> {
            return try {
                val arr = JSONArray(jsonStr)
                val result = mutableListOf<SubjectAttendance>()
                for (i in 0 until arr.length()) {
                    result.add(fromJson(arr.getJSONObject(i)))
                }
                result
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
