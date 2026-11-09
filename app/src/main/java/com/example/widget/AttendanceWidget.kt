package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.data.model.AttendanceSummary
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = UserPreferences(context)
        val summary = prefs.cachedAttendanceFlow.first()
        val threshold = prefs.targetThresholdFlow.first()
        val username = prefs.usernameFlow.first()

        provideContent {
            GlanceTheme {
                WidgetRoot(
                    context = context,
                    summary = summary,
                    threshold = threshold,
                    username = username
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetRoot(
        context: Context,
        summary: AttendanceSummary?,
        threshold: Double,
        username: String?
    ) {
        val backgroundColor = Color(0xFF0D1B2A)
        val surfaceColor = Color(0xFF1B263B)
        val textPrimary = Color(0xFFFFFFFF)
        val textSecondary = Color(0xFFB0BEC5)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            contentAlignment = Alignment.TopStart
        ) {
            if (username.isNullOrEmpty() || summary == null) {
                // Not logged in or no data yet
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clg 1%",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFF64B5F6)),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = if (username.isNullOrEmpty()) "Tap to sign in" else "Tap to sync attendance",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(textSecondary),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                // Logged in with attendance data
                val percentage = summary.percentage
                val targetPercent = (threshold * 100).toInt()
                val statusColor = when {
                    percentage >= targetPercent -> Color(0xFF4CAF50)
                    percentage >= (targetPercent - 5) -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                }

                val safeMiss = summary.safeToMiss(threshold)
                val hoursRecover = summary.hoursToRecover(threshold)
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(Date(summary.lastSyncedMillis))

                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Top Bar: Title, Student ID, Refresh action
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.clg1_logo),
                            contentDescription = "Logo",
                            modifier = GlanceModifier.size(24.dp).cornerRadius(6.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "CLG 1% ATTENDANCE",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFF90CAF9)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = username.uppercase(Locale.getDefault()),
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(textSecondary),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        // Refresh button
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(8.dp)
                                .background(surfaceColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<RefreshAttendanceAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↻ Refresh",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFF90CAF9)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Middle Section: Large Percentage + Present Ratio
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attendance Percentage
                        Text(
                            text = summary.formattedPercentage,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(statusColor),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        Column {
                            Text(
                                text = "${summary.present}/${summary.marked} hrs",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(textPrimary),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Absent: ${summary.absent} hrs",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFFF8A80)),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Bottom Pill: Safe bunk or recovery hours
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .background(
                                if (safeMiss > 0) Color(0xFF1B382B) else Color(0xFF381B1B)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    safeMiss > 0 -> "Safe to miss: $safeMiss hrs"
                                    hoursRecover > 0 -> "Must attend: $hoursRecover hrs (for $targetPercent%)"
                                    else -> "On target ($targetPercent%)"
                                },
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(
                                        if (safeMiss > 0) Color(0xFF81C784) else Color(0xFFFFAB91)
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )

                            Text(
                                text = timeStr,
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(textSecondary),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
