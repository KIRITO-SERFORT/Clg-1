package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppUpdateInfo(
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false
)

@Composable
fun AppUpdateChecker() {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("app_config")
                .document("latest_release")
                .get()
                .await()

            if (doc.exists()) {
                val latestCode = doc.getLong("latestVersionCode") ?: 0L
                val latestName = doc.getString("latestVersionName") ?: ""
                val apkUrl = doc.getString("apkUrl") ?: ""
                val releaseNotes = doc.getString("releaseNotes") ?: ""
                val forceUpdate = doc.getBoolean("forceUpdate") ?: false

                if (latestCode > BuildConfig.VERSION_CODE && apkUrl.isNotBlank()) {
                    updateInfo = AppUpdateInfo(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        apkUrl = apkUrl,
                        releaseNotes = releaseNotes,
                        forceUpdate = forceUpdate
                    )
                }
            }
        } catch (_: Exception) {
            // Silently ignore if offline or Firestore is unavailable
        }
    }

    val info = updateInfo
    if (info != null && !dismissed) {
        AlertDialog(
            onDismissRequest = {
                if (!info.forceUpdate) {
                    dismissed = true
                }
            },
            title = {
                Text(
                    text = if (info.latestVersionName.isNotBlank()) {
                        "Update Available (v${info.latestVersionName})"
                    } else {
                        "Update Available"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new version of Clg 1% is ready to download.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "What's new:\n${info.releaseNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "Unable to open download link.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.testTag("update_now_button")
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = if (!info.forceUpdate) {
                {
                    TextButton(
                        onClick = { dismissed = true },
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text("Later")
                    }
                }
            } else {
                null
            }
        )
    }
}
