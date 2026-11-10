package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class AbsentSession(
    val session: String,
    val startTime: String,
    val endTime: String,
    val courseCode: String,
    val courseTitle: String
) {
    val timeRange: String
        get() = "$startTime - $endTime"
}

data class AbsentDayRecord(
    val rawDate: String,
    val formattedDate: String,
    val sessions: List<AbsentSession>
) {
    companion object {
        fun fromJsonArray(array: JSONArray): List<AbsentDayRecord> {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }

            val result = mutableListOf<AbsentDayRecord>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawDate = obj.optString("date", "")
                val formattedDate = try {
                    val parsed = isoFormat.parse(rawDate)
                    if (parsed != null) displayFormat.format(parsed) else rawDate
                } catch (_: Exception) {
                    rawDate
                }

                val sessionsArr = obj.optJSONArray("sessions") ?: JSONArray()
                val sessions = mutableListOf<AbsentSession>()
                for (j in 0 until sessionsArr.length()) {
                    val sObj: JSONObject = sessionsArr.optJSONObject(j) ?: continue
                    sessions.add(
                        AbsentSession(
                            session = sObj.optString("session", ""),
                            startTime = sObj.optString("startTime", ""),
                            endTime = sObj.optString("endTime", ""),
                            courseCode = sObj.optString("courseCode", ""),
                            courseTitle = sObj.optString("courseTitle", "")
                        )
                    )
                }

                result.add(
                    AbsentDayRecord(
                        rawDate = rawDate,
                        formattedDate = formattedDate,
                        sessions = sessions
                    )
                )
            }
            return result
        }
    }
}
