package com.example.data.network

import com.example.data.model.AbsentDayRecord
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.example.data.model.Timetable
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ErpException(message: String) : IOException(message) {
    class BadCredentials : ErpException("Login rejected: wrong register number/password or account locked")
    class PortalChanged(detail: String) : ErpException("Portal changed: $detail")
    class ServerError(detail: String) : ErpException("Server error: $detail")
}

data class FullAttendanceResult(
    val summary: AttendanceSummary,
    val subjects: List<SubjectAttendance>,
    val studentName: String
)

class ErpClient : ErpConnector {

    override val id: String = ErpConnectors.DEFAULT_ID
    override val shortName: String = ErpConnectors.DEFAULT_SHORT_NAME

    private companion object {
        const val AUTH_BASE = "https://auth.tnedu.in/realms/accet/protocol/openid-connect"
        const val PORTAL = "https://accet.tnedu.in"
        const val CLIENT_ID = "student"
        const val REDIRECT_URI = "https://accet.tnedu.in/student/"
    }

    private class Tokens(val accessToken: String, val refreshToken: String)

    private class MemoryCookieJar : CookieJar {
        private val store = mutableListOf<Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store.removeAll { old ->
                cookies.any { it.name == old.name && it.domain == old.domain && it.path == old.path }
            }
            store.addAll(cookies)
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store.filter { it.matches(url) && it.expiresAt > System.currentTimeMillis() }
    }

    // Redirects handled manually to capture authorization code from Location header
    private val http = OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar())
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    /** Full login -> attendance fetch -> logout. Use a fresh ErpClient per call. */
    fun fetchAttendance(username: String, password: String): AttendanceSummary {
        val result = fetchFullAttendance(username, password)
        return result.summary
    }

    /** Fetches detailed absent days and sessions from /student/attendance/absent/<rollNo>. */
    fun fetchAbsentDays(username: String, password: String): List<AbsentDayRecord> {
        val cleanUser = username.trim()
        val tokens = login(cleanUser, password)
        return try {
            getAbsentDetails(cleanUser, tokens.accessToken)
        } finally {
            runCatching { logout(tokens.refreshToken) }
        }
    }

    /** Fetches both overall college totals and detailed subject-wise records. */
    override fun fetchFullAttendance(username: String, password: String): FullAttendanceResult {
        val cleanUser = username.trim()
        val tokens = login(cleanUser, password)
        try {
            val summary = getAttendance(cleanUser, tokens.accessToken)
            val (subjects, studentName) = getSubjectWiseAttendance(cleanUser, tokens.accessToken)
            return FullAttendanceResult(summary, subjects, studentName)
        } finally {
            runCatching { logout(tokens.refreshToken) }
        }
    }

    /**
     * Fetches the class timetable: login -> class id (nodeId from the student profile)
     * -> timetable -> logout. Use a fresh ErpClient per call.
     */
    override fun fetchTimetable(username: String, password: String): Timetable {
        val cleanUser = username.trim()
        val tokens = login(cleanUser, password)
        try {
            val nodeId = getClassNodeId(cleanUser, tokens.accessToken)
            return getTimetable(nodeId, cleanUser, tokens.accessToken)
        } finally {
            runCatching { logout(tokens.refreshToken) }
        }
    }

    /**
     * GET /student/students/{registerNumber} returns the whole student profile.
     * We read ONLY "nodeId" (the class/section id used by the timetable URL) and
     * throw everything else away. Never log, store or display the rest of the profile.
     */
    private fun getClassNodeId(username: String, accessToken: String): String {
        val request = Request.Builder()
            .url("$PORTAL/student/students/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw ErpException.ServerError("profile API HTTP ${resp.code}")
            val nodeId = JSONObject(resp.body?.string().orEmpty()).optString("nodeId", "")
            if (nodeId.isBlank()) throw ErpException.PortalChanged("class id (nodeId) not found")
            return nodeId
        }
    }

    private fun getTimetable(nodeId: String, username: String, accessToken: String): Timetable {
        val request = Request.Builder()
            .url("$PORTAL/student/time/table/$nodeId/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw ErpException.ServerError("timetable API HTTP ${resp.code}")
            val root = JSONObject(resp.body?.string().orEmpty())
            return Timetable.fromErpResponse(root)
        }
    }

    private fun login(username: String, password: String): Tokens {
        // 1) Load login page; session_code/execution/tab_id are in form action
        val authUrl = "$AUTH_BASE/auth".toHttpUrl().newBuilder()
            .addQueryParameter("client_id", CLIENT_ID)
            .addQueryParameter("redirect_uri", REDIRECT_URI)
            .addQueryParameter("state", UUID.randomUUID().toString())
            .addQueryParameter("response_mode", "fragment")
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", "openid")
            .addQueryParameter("nonce", UUID.randomUUID().toString())
            .build()

        val loginHtml = http.newCall(Request.Builder().url(authUrl).build()).execute().use { resp ->
            if (resp.code != 200) throw ErpException.PortalChanged("authorize returned ${resp.code}")
            resp.body?.string() ?: throw ErpException.PortalChanged("empty login page")
        }

        val formAction = Jsoup.parse(loginHtml, authUrl.toString())
            .selectFirst("form#kc-form-login")
            ?.absUrl("action")
        if (formAction.isNullOrEmpty()) throw ErpException.PortalChanged("login form not found")

        // 2) Submit credentials. 302 redirect on success; 200 on login page redisplayed with error.
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("credentialId", "")
            .build()

        val location = http.newCall(Request.Builder().url(formAction).post(body).build()).execute().use { resp ->
            when (resp.code) {
                302 -> resp.header("Location") ?: throw ErpException.PortalChanged("redirect without Location header")
                200 -> throw ErpException.BadCredentials()
                else -> throw ErpException.ServerError("login POST returned HTTP ${resp.code}")
            }
        }

        // 3) The code arrives in URL fragment or query parameter
        val fragment = location.substringAfter('#', "")
        val code = (if (fragment.isNotEmpty()) "https://x/?$fragment".toHttpUrlOrNull() else null)
            ?.queryParameter("code")
            ?: location.toHttpUrlOrNull()?.queryParameter("code")
            ?: throw ErpException.PortalChanged("no authorization code in redirect")

        // 4) Exchange authorization code for access and refresh tokens
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", CLIENT_ID)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        http.newCall(Request.Builder().url("$AUTH_BASE/token").post(form).build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw ErpException.ServerError("token endpoint HTTP ${resp.code}")
            val json = JSONObject(text)
            return Tokens(json.getString("access_token"), json.optString("refresh_token"))
        }
    }

    private fun getAttendance(username: String, accessToken: String): AttendanceSummary {
        val request = Request.Builder()
            .url("$PORTAL/student/attendance/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw ErpException.ServerError("attendance API HTTP ${resp.code}")
            val bodyText = resp.body?.string().orEmpty()
            val j = JSONObject(bodyText)
            return AttendanceSummary(
                present = j.optInt("presentCountTillDate", 0),
                absent = j.optInt("absentCountTillDate", 0),
                marked = j.optInt("enteredTillDate", 0),
                notMarked = j.optInt("notEnteredTillDate", 0),
                totalWorking = j.optInt("totalworkingTillDate", 0),
                apiPercentage = j.optInt("netPresentPercentageTillDate", 0),
                tillDate = j.optString("tillDate", ""),
                lastSyncedMillis = System.currentTimeMillis()
            )
        }
    }

    private fun getSubjectWiseAttendance(username: String, accessToken: String): Pair<List<SubjectAttendance>, String> {
        val request = Request.Builder()
            .url("$PORTAL/student/attendance/subject/wise/record/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        return try {
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return Pair(emptyList(), "")
                val bodyText = resp.body?.string().orEmpty()
                val root = JSONObject(bodyText)
                val dataObj = root.optJSONObject("data") ?: return Pair(emptyList(), "")
                val studentName = dataObj.optString("name", "")
                val arr = dataObj.optJSONArray("attendanceDetails") ?: return Pair(emptyList(), studentName)

                val subjects = mutableListOf<SubjectAttendance>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    subjects.add(SubjectAttendance.fromErpDetail(item))
                }
                Pair(subjects, studentName)
            }
        } catch (_: Exception) {
            Pair(emptyList(), "")
        }
    }

    private fun getAbsentDetails(username: String, accessToken: String): List<AbsentDayRecord> {
        val request = Request.Builder()
            .url("$PORTAL/student/attendance/absent/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        return try {
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val bodyText = resp.body?.string().orEmpty().trim()
                if (!bodyText.startsWith("[")) return emptyList()
                AbsentDayRecord.fromJsonArray(JSONArray(bodyText))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun logout(refreshToken: String) {
        if (refreshToken.isEmpty()) return
        val form = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("refresh_token", refreshToken)
            .build()
        http.newCall(Request.Builder().url("$AUTH_BASE/logout").post(form).build()).execute().close()
    }
}
