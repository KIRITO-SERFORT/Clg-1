package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.data.model.AttendanceSummary
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

class CompactAttendanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = UserPreferences(context)
        val summary = prefs.cachedAttendanceFlow.first()
        val username = prefs.usernameFlow.first()
        val threshold = prefs.targetThresholdFlow.first()

        provideContent {
            GlanceTheme {
                CompactWidgetContent(context = context, summary = summary, username = username, threshold = threshold)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CompactWidgetContent(
        context: Context,
        summary: AttendanceSummary?,
        username: String?,
        threshold: Double
    ) {
        val backgroundColor = Color(0xFF0F172A)
        val cardColor = Color(0xFF1E293B)
        val textPrimary = Color(0xFFFFFFFF)
        val textSecondary = Color(0xFF94A3B8)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(16.dp)
                .padding(10.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            contentAlignment = Alignment.Center
        ) {
            if (username.isNullOrEmpty() || summary == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.clg1_logo),
                        contentDescription = "Logo",
                        modifier = GlanceModifier.size(36.dp).cornerRadius(8.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Clg 1%",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Tap to login",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(textSecondary),
                            fontSize = 10.sp
                        )
                    )
                }
            } else {
                val pieBitmap = createPieChartBitmap(
                    present = summary.present,
                    absent = summary.absent,
                    open = summary.notMarked,
                    percentage = summary.percentage
                )

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top header: Title and quick refresh icon
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            // Text (not just colour) tells the student the numbers may be out of date.
                            text = if (WidgetFreshness.isStale(summary.lastSyncedMillis)) "Clg 1% · old data" else "Clg 1%",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        Text(
                            text = "↻",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .clickable(actionRunCallback<RefreshAttendanceAction>())
                                .padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Pie Chart with percentage in center
                    Box(
                        modifier = GlanceModifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(pieBitmap),
                            contentDescription = "Attendance Pie Chart",
                            modifier = GlanceModifier.size(80.dp)
                        )

                        Text(
                            text = "${summary.percentage.toInt()}%",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(
                                    if (summary.percentage >= threshold * 100.0) Color(0xFF4ADE80) else Color(0xFFF87171)
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Absent Count Pill
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(12.dp)
                            .background(cardColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${summary.absent} hrs absent",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFFCA5A5)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createPieChartBitmap(
        present: Int,
        absent: Int,
        open: Int,
        percentage: Double,
        sizePx: Int = 180
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val total = (present + absent + open).coerceAtLeast(1).toFloat()
        val presentAngle = (present / total) * 360f
        val absentAngle = (absent / total) * 360f

        val strokeWidth = 20f
        val padding = strokeWidth / 2f + 4f
        val rect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        // Background track (Open / Remaining)
        paint.color = android.graphics.Color.parseColor("#334155")
        canvas.drawArc(rect, 0f, 360f, false, paint)

        var startAngle = -90f

        // Present arc (Green)
        if (present > 0) {
            paint.color = android.graphics.Color.parseColor("#22C55E")
            canvas.drawArc(rect, startAngle, presentAngle, false, paint)
            startAngle += presentAngle
        }

        // Absent arc (Red)
        if (absent > 0) {
            paint.color = android.graphics.Color.parseColor("#EF4444")
            canvas.drawArc(rect, startAngle, absentAngle, false, paint)
        }

        return bitmap
    }
}
