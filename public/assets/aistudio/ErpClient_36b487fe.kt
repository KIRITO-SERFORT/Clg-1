package com.example.attendance

// Gradle (app module):
//   implementation("com.squareup.okhttp3:okhttp:4.12.0")
//   implementation("org.jsoup:jsoup:1.17.2")
// Manifest: <uses-permission android:name="android.permission.INTERNET" />
//
// Usage (always off the main thread, e.g. inside a CoroutineWorker):
//   val summary = withContext(Dispatchers.IO) { ErpClient().fetchAttendance(user, pass) }

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor

// ---------- Data ----------

data class AttendanceSummary(
    val present: Int,        // presentCountTillDate
    val absent: Int,         // absentCountTillDate
    val marked: Int,         // enteredTillDate
    val notMarked: Int,      // notEnteredTillDate
    val totalWorking: Int,   // totalworkingTillDate
    val apiPercentage: Int,  // netPresentPercentageTillDate (rounded by server)
    val tillDate: String     // tillDate (ISO string)
) {
    /** Exact percentage, calculated on marked hours (matches the portal: 150/163 = 92%). */
    val percentage: Double
        get() = if (marked == 0) 0.0 else present * 100.0 / marked

    /** Hours you can still miss and stay at or above [threshold] (default 75%). */
    fun safeToMiss(threshold: Double = 0.75): Int =
        floor(present / threshold - marked + 1e-9).toInt().coerceAtLeast(0)

    /** Consecutive hours you must attend to climb back to [threshold]. 0 if already above. */
    fun hoursToRecover(threshold: Double = 0.75): Int {
        if (marked == 0 || present.toDouble() / marked >= threshold) return 0
        return ceil((threshold * marked - present) / (1 - threshold) - 1e-9).toInt()
    }
}

sealed class ErpException(message: String) : IOException(message) {
    class BadCredentials : ErpException("Login rejected: wrong username/password or account locked")
    class PortalChanged(detail: String) : ErpException("Portal changed: $detail")
    class ServerError(detail: String) : ErpException("Server error: $detail")
}

// ---------- Client ----------

class ErpClient {

    private companion object {
        const val AUTH_BASE = "https://auth.tnedu.in/realms/accet/protocol/openid-connect"
        const val PORTAL = "https://accet.tnedu.in"

        // The attendance page's token has azp = "student". If login fails with
        // "invalid redirect", capture the real authorize URL for client_id=student
        // in Chrome DevTools and correct REDIRECT_URI.
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

    // Redirects are handled by hand: we must read the Location header that carries the auth code.
    private val http = OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar())
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Full login -> attendance fetch -> logout. Use a fresh ErpClient per call. */
    fun fetchAttendance(username: String, password: String): AttendanceSummary {
        val tokens = login(username.trim(), password)
        try {
            return getAttendance(username.trim(), tokens.accessToken)
        } finally {
            runCatching { logout(tokens.refreshToken) } // best effort, keeps sessions tidy
        }
    }

    // ----- Step 1-4: Keycloak authorization-code login -----

    private fun login(username: String, password: String): Tokens {
        // 1) Load the login page; session_code/execution/tab_id are one-time values in the form action.
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

        // 2) Submit credentials. Success = 302 redirect; 200 = login page again (error).
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("credentialId", "")
            .build()

        val location = http.newCall(Request.Builder().url(formAction).post(body).build()).execute().use { resp ->
            when (resp.code) {
                302 -> resp.header("Location") ?: throw ErpException.PortalChanged("redirect without Location")
                200 -> throw ErpException.BadCredentials()
                else -> throw ErpException.ServerError("login POST returned ${resp.code}")
            }
        }

        // 3) The code arrives in the URL fragment (response_mode=fragment); fall back to the query string.
        val fragment = location.substringAfter('#', "")
        val code = (if (fragment.isNotEmpty()) "https://x/?$fragment".toHttpUrlOrNull() else null)
            ?.queryParameter("code")
            ?: location.toHttpUrlOrNull()?.queryParameter("code")
            ?: throw ErpException.PortalChanged("no code in redirect")

        // 4) Exchange the code for tokens.
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", CLIENT_ID)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        http.newCall(Request.Builder().url("$AUTH_BASE/token").post(form).build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw ErpException.ServerError("token endpoint ${resp.code}")
            val json = JSONObject(text)
            return Tokens(json.getString("access_token"), json.optString("refresh_token"))
        }
    }

    // ----- Step 5: attendance API -----

    private fun getAttendance(username: String, accessToken: String): AttendanceSummary {
        // The register number is the login username (the API path uses it).
        val request = Request.Builder()
            .url("$PORTAL/student/attendance/$username")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw ErpException.ServerError("attendance API ${resp.code}")
            val j = JSONObject(resp.body?.string().orEmpty())
            return AttendanceSummary(
                present = j.getInt("presentCountTillDate"),
                absent = j.getInt("absentCountTillDate"),
                marked = j.getInt("enteredTillDate"),
                notMarked = j.getInt("notEnteredTillDate"),
                totalWorking = j.getInt("totalworkingTillDate"),
                apiPercentage = j.getInt("netPresentPercentageTillDate"),
                tillDate = j.optString("tillDate")
            )
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
