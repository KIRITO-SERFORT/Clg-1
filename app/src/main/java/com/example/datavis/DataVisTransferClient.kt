package com.example.datavis

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Explicitly uploads only after the user confirms the export preview. */
object DataVisTransferClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = BuildConfig.DATAVIS_TRANSFER_API_BASE.startsWith("https://")

    /** Uploads CSV to a short-lived single-use transfer endpoint and opens DataVis with the token. */
    fun uploadAndOpen(context: Context, csv: String): String {
        val base = BuildConfig.DATAVIS_TRANSFER_API_BASE.trimEnd('/')
        require(base.startsWith("https://")) { "Secure DataVis transfer API is not configured. Use Share CSV for manual upload." }
        require(csv.toByteArray(Charsets.UTF_8).size <= 2 * 1024 * 1024) { "CSV exceeds the 2 MB transfer limit" }
        val body = csv.toRequestBody("text/csv; charset=utf-8".toMediaType())
        val request = Request.Builder().url("$base/v1/transfers").header("X-CSV-Schema", "1").post(body).build()
        val response = http.newCall(request).execute()
        return try {
            val payload = response.body?.string().orEmpty()
            require(response.isSuccessful) { "Transfer service rejected the export (${response.code})" }
            val token = JSONObject(payload).optString("import_token")
            require(token.length >= 40) { "Transfer service returned an invalid token" }
            val target = Uri.parse("https://datavis-v5.streamlit.app/").buildUpon().appendQueryParameter("import_token", token).build()
            context.startActivity(Intent(Intent.ACTION_VIEW, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "CSV transferred. The one-time import token expires in 10 minutes and is consumed once by DataVis."
        } finally {
            response.close()
        }
    }
}
