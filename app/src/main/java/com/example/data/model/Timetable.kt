package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

/** One cell of the timetable grid: which course(s) run in [session] on day order [day]. */
data class TimetablePeriod(
    val day: Int,
    val session: Int,
    val codes: List<String>,
    val titles: List<String>,
    val faculty: List<String>
) {
    val primaryCode: String get() = codes.firstOrNull().orEmpty()
    val primaryTitle: String get() = titles.firstOrNull().orEmpty()
}

data class Timetable(
    val noOfDays: Int,
    val noOfSessions: Int,
    val periods: List<TimetablePeriod>,
    /** session number -> "09:20-10:10" */
    val sessionTimes: Map<Int, String>,
    val fetchedAtMillis: Long = System.currentTimeMillis()
) {
    fun periodsFor(day: Int): List<TimetablePeriod> =
        periods.filter { it.day == day }.sortedBy { it.session }

    fun maxSessionFor(day: Int): Int = periodsFor(day).maxOfOrNull { it.session } ?: 0

    /** Start/end of a session in minutes after midnight, or null if unknown. */
    fun sessionMinutes(session: Int): Pair<Int, Int>? {
        val raw = sessionTimes[session] ?: return null
        val parts = raw.split("-")
        if (parts.size != 2) return null
        val start = toMinutes(parts[0]) ?: return null
        val end = toMinutes(parts[1]) ?: return null
        return Pair(start, end)
    }

    fun startLabel(session: Int): String =
        sessionTimes[session]?.substringBefore("-")?.trim().orEmpty()

    fun toJson(): String {
        val root = JSONObject()
        root.put("noOfDays", noOfDays)
        root.put("noOfSessions", noOfSessions)
        root.put("fetchedAtMillis", fetchedAtMillis)
        val arr = JSONArray()
        periods.forEach { p ->
            val o = JSONObject()
            o.put("day", p.day)
            o.put("session", p.session)
            o.put("codes", JSONArray(p.codes))
            o.put("titles", JSONArray(p.titles))
            o.put("faculty", JSONArray(p.faculty))
            arr.put(o)
        }
        root.put("periods", arr)
        val times = JSONObject()
        sessionTimes.forEach { (k, v) -> times.put(k.toString(), v) }
        root.put("sessionTimes", times)
        return root.toString()
    }

    companion object {
        private fun toMinutes(text: String): Int? {
            val p = text.trim().split(":")
            if (p.size != 2) return null
            val h = p[0].trim().toIntOrNull() ?: return null
            val m = p[1].trim().toIntOrNull() ?: return null
            return h * 60 + m
        }

        private fun numberFrom(key: String, prefix: String): Int? =
            key.trim().lowercase().removePrefix(prefix).trim().toIntOrNull()

        private fun strings(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) out.add(arr.optString(i, ""))
            return out
        }

        fun fromJson(text: String): Timetable? = try {
            val root = JSONObject(text)
            val periods = mutableListOf<TimetablePeriod>()
            val arr = root.optJSONArray("periods") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                periods.add(
                    TimetablePeriod(
                        day = o.getInt("day"),
                        session = o.getInt("session"),
                        codes = strings(o.optJSONArray("codes")),
                        titles = strings(o.optJSONArray("titles")),
                        faculty = strings(o.optJSONArray("faculty"))
                    )
                )
            }
            val times = mutableMapOf<Int, String>()
            val tObj = root.optJSONObject("sessionTimes")
            if (tObj != null) {
                val keys = tObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val n = k.toIntOrNull() ?: continue
                    times[n] = tObj.optString(k, "")
                }
            }
            Timetable(
                noOfDays = root.optInt("noOfDays", 5),
                noOfSessions = root.optInt("noOfSessions", 8),
                periods = periods,
                sessionTimes = times,
                fetchedAtMillis = root.optLong("fetchedAtMillis", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }

        /**
         * Parses the ERP response of GET /student/time/table/{nodeId}/{registerNumber}.
         * Keys like "day1"/"session3" carry the numbers; slots arrive unsorted; the
         * sessionTimings keys and values contain stray spaces ("session1 ": "09:20 -10:10 ").
         */
        fun fromErpResponse(root: JSONObject): Timetable {
            val noOfDays = root.optInt("noOfDays", 5).coerceAtLeast(1)
            val noOfSessions = root.optInt("noOfSessions", 8).coerceAtLeast(1)

            val periods = mutableListOf<TimetablePeriod>()
            val arr = root.optJSONArray("timeTable") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val day = numberFrom(item.optString("dayKey", ""), "day") ?: continue
                val session = numberFrom(item.optString("sessionKey", ""), "session") ?: continue
                val slots = item.optJSONArray("slot") ?: continue

                val codes = mutableListOf<String>()
                val titles = mutableListOf<String>()
                val faculty = mutableListOf<String>()
                for (s in 0 until slots.length()) {
                    val slot = slots.optJSONObject(s) ?: continue
                    val course = slot.optJSONObject("courseDetails")
                    val code = course?.optString("courseCode", "")?.trim().orEmpty()
                    if (code.isNotEmpty()) {
                        codes.add(code)
                        titles.add(course?.optString("courseTitle", "")?.trim().orEmpty())
                    }
                    val facArr = slot.optJSONArray("facultyDetails")
                    if (facArr != null) {
                        for (f in 0 until facArr.length()) {
                            val name = facArr.optJSONObject(f)?.optString("facultyName", "")?.trim().orEmpty()
                            if (name.isNotEmpty()) faculty.add(name)
                        }
                    }
                }
                if (codes.isEmpty()) continue
                periods.add(TimetablePeriod(day, session, codes, titles, faculty))
            }

            val times = mutableMapOf<Int, String>()
            val tObj = root.optJSONObject("sessionTimings")
            if (tObj != null) {
                val keys = tObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val n = numberFrom(k, "session") ?: continue
                    times[n] = tObj.optString(k, "").replace(" ", "")
                }
            }

            return Timetable(
                noOfDays = noOfDays,
                noOfSessions = noOfSessions,
                periods = periods.distinctBy { it.day to it.session },
                sessionTimes = times
            )
        }
    }
}
