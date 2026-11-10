package com.example.data.analytics

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.security.SecretCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ByokProvider(val id: String, val label: String, val hint: String) {
    GEMINI("gemini", "Gemini", "AIza…"),
    CHATGPT("chatgpt", "ChatGPT", "sk-…"),
    DEEPSEEK("deepseek", "DeepSeek", "sk-…"),
    CLAUDE("claude", "Claude", "sk-ant-…");

    companion object {
        fun fromId(id: String?): ByokProvider =
            entries.firstOrNull { it.id == id } ?: GEMINI
    }
}

/** Encrypted BYOK prefs. Never log the key. */
object ByokStore {
    private const val PREFS = "clg1_byok"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_SECRET = "secret"

    fun loadProvider(context: Context): ByokProvider {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROVIDER, ByokProvider.GEMINI.id)
        return ByokProvider.fromId(id)
    }

    fun hasKey(context: Context): Boolean = loadKey(context) != null

    fun loadKey(context: Context): String? {
        val blob = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SECRET, null) ?: return null
        return SecretCipher.decrypt(blob)?.takeIf { it.isNotBlank() }
    }

    fun save(context: Context, provider: ByokProvider, apiKey: String) {
        val trimmed = apiKey.trim()
        require(trimmed.isNotEmpty()) { "API key is empty." }
        val encrypted = SecretCipher.encrypt(trimmed)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROVIDER, provider.id)
            .putString(KEY_SECRET, encrypted)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/**
 * Sends the same advisor prompt to the student's own provider.
 * Falls back is the caller's job (existing Firebase Gemini) when no key is saved.
 */
class ByokAdvisorClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(55, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun complete(provider: ByokProvider, apiKey: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            val request = when (provider) {
                ByokProvider.GEMINI -> gemini(apiKey, prompt)
                ByokProvider.CHATGPT -> openaiCompatible(
                    url = "https://api.openai.com/v1/chat/completions",
                    model = "gpt-4o-mini",
                    apiKey = apiKey,
                    prompt = prompt,
                )
                ByokProvider.DEEPSEEK -> openaiCompatible(
                    url = "https://api.deepseek.com/chat/completions",
                    model = "deepseek-chat",
                    apiKey = apiKey,
                    prompt = prompt,
                )
                ByokProvider.CLAUDE -> claude(apiKey, prompt)
            }
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(provider.label + " error " + response.code)
                }
                parse(provider, body)
            }
        }

    private fun gemini(apiKey: String, prompt: String): Request {
        val json = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", prompt))
            )))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        return Request.Builder().url(url)
            .post(json.toString().toRequestBody(JSON))
            .build()
    }

    private fun openaiCompatible(url: String, model: String, apiKey: String, prompt: String): Request {
        val json = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        return Request.Builder().url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody(JSON))
            .build()
    }

    private fun claude(apiKey: String, prompt: String): Request {
        val json = JSONObject()
            .put("model", "claude-sonnet-4-5")
            .put("max_tokens", 1200)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        return Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(json.toString().toRequestBody(JSON))
            .build()
    }

    private fun parse(provider: ByokProvider, body: String): String {
        val root = JSONObject(body)
        val text = when (provider) {
            ByokProvider.GEMINI -> root.getJSONArray("candidates")
                .getJSONObject(0).getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0).getString("text")
            ByokProvider.CHATGPT, ByokProvider.DEEPSEEK -> root.getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
            ByokProvider.CLAUDE -> root.getJSONArray("content")
                .getJSONObject(0).getString("text")
        }
        if (text.isBlank()) throw IllegalStateException(provider.label + " returned an empty report.")
        return text.trim()
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

/** Row title + gear. Put this where the AI analytics action is labelled. */
@Composable
fun AiAnalyticsTitleRow(
    context: Context,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("AI analytics", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.Settings, contentDescription = "AI key settings")
        }
    }
    if (open) {
        ByokKeyDialog(context = context, onDismiss = { open = false })
    }
}

@Composable
fun ByokKeyDialog(context: Context, onDismiss: () -> Unit) {
    var provider by remember { mutableStateOf(ByokStore.loadProvider(context)) }
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val saved = remember { ByokStore.hasKey(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your AI key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (saved) "A key is saved on this phone. Paste a new one to replace it."
                    else "Optional. If empty, AI analytics keeps using the built-in Gemini.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ByokProvider.entries.forEach { item ->
                        FilterChip(
                            selected = provider == item,
                            onClick = { provider = item },
                            label = { Text(item.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(provider.label + " API key") },
                    placeholder = { Text(provider.hint) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    ByokStore.save(context, provider, key)
                    onDismiss()
                } catch (e: Exception) {
                    error = e.message ?: "Could not save the key."
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (saved) {
                    TextButton(onClick = { ByokStore.clear(context); onDismiss() }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
