package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.example.MainActivity
import com.example.R
import com.example.data.model.AttendanceSummary
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * Royal mini widget: deep-purple gradient card, thin gold border, round gold-and-lavender
 * chart, the app logo in a gold ring, and "Present x%" / "Absent y%" in a serif face.
 *
 * The whole card is drawn into one bitmap at the widget's real size (SizeMode.Exact), so the
 * gradients, gold border and fonts stay crisp. Tap anywhere to open the app.
 */
class MiniPurpleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = UserPreferences(context)
        val summary = prefs.cachedAttendanceFlow.first()
        val username = prefs.usernameFlow.first()

        provideContent {
            GlanceTheme {
                RoyalContent(context = context, summary = summary, username = username)
            }
        }
    }

    @Composable
    private fun RoyalContent(context: Context, summary: AttendanceSummary?, username: String?) {
        val size = LocalSize.current
        val bitmap = renderRoyalBitmap(
            context = context,
            widthDp = size.width.value,
            heightDp = size.height.value,
            summary = summary,
            loggedIn = !username.isNullOrEmpty()
        )
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = descriptionFor(summary, username),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        )
    }

    private fun descriptionFor(summary: AttendanceSummary?, username: String?): String {
        if (username.isNullOrEmpty() || summary == null || summary.marked == 0) return "Clg 1% attendance widget"
        val present = Math.round(summary.percentage).toInt().coerceIn(0, 100)
        return "Present $present percent, absent ${100 - present} percent"
    }

    private fun hex(color: String): Int = Color.parseColor(color)

    private fun renderRoyalBitmap(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        summary: AttendanceSummary?,
        loggedIn: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        var w = (widthDp * density).coerceAtLeast(160f)
        var h = (heightDp * density).coerceAtLeast(80f)
        val shrink = minOf(1f, 1100f / w, 520f / h) // keep the bitmap small for RemoteViews
        w *= shrink
        h *= shrink
        val unit = density * shrink // pixels per dp in this bitmap

        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = 22f * unit
        val centerY = h / 2f

        // ---- royal background ----
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(hex("#3B0764"), hex("#2E1065"), hex("#1E1B4B")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, backgroundPaint)

        // soft light on the upper half
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.6f,
                hex("#2EFFFFFF"), hex("#00FFFFFF"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, w, h * 0.6f), radius, radius, highlightPaint)

        // thin gold border
        val borderInset = 1.2f * unit
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * unit
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(hex("#F9E7A6"), hex("#B8860B"), hex("#F9E7A6")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(borderInset, borderInset, w - borderInset, h - borderInset),
            radius - borderInset, radius - borderInset, borderPaint
        )

        val padding = 12f * unit
        var x = padding
        val wide = widthDp >= 170f

        // ---- logo in a gold ring ----
        if (wide) {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.clg1_logo)
            if (logo != null) {
                val d = minOf(h * 0.62f, 46f * unit)
                val shader = BitmapShader(logo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = Matrix()
                val scale = d / logo.width
                matrix.setScale(scale, scale)
                matrix.postTranslate(x, centerY - d / 2f)
                shader.setLocalMatrix(matrix)
                val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                canvas.drawCircle(x + d / 2f, centerY, d / 2f, logoPaint)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.6f * unit
                    this.shader = LinearGradient(
                        x, centerY - d / 2f, x + d, centerY + d / 2f,
                        hex("#F9E7A6"), hex("#B8860B"), Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(x + d / 2f, centerY, d / 2f - 0.8f * unit, ringPaint)
                x += d + 10f * unit
            }
        }

        val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val availableWidth = w - x - padding

        // ---- no data yet ----
        if (!loggedIn || summary == null || summary.marked == 0) {
            val message = if (!loggedIn) "Tap to log in" else "No data yet"
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = serifBold
                textSize = minOf(h * 0.26f, 17f * unit)
                color = hex("#F9E7A6")
            }
            val measured = paint.measureText(message)
            if (measured > availableWidth) paint.textSize *= availableWidth / measured
            val baseline = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
            canvas.drawText(message, x, baseline, paint)
            return bitmap
        }

        // ---- round chart ----
        val presentPct = Math.round(summary.percentage).toInt().coerceIn(0, 100)
        val absentPct = 100 - presentPct

        val diameter = minOf(h * 0.74f, 52f * unit)
        val stroke = diameter * 0.17f
        val chartLeft = x
        val chartTop = centerY - diameter / 2f
        val arcRect = RectF(
            chartLeft + stroke / 2f, chartTop + stroke / 2f,
            chartLeft + diameter - stroke / 2f, chartTop + diameter - stroke / 2f
        )
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.BUTT
        }
        arcPaint.color = hex("#4C1D95")
        canvas.drawArc(arcRect, 0f, 360f, false, arcPaint)

        val total = (summary.present + summary.absent).coerceAtLeast(1).toFloat()
        val presentSweep = summary.present / total * 360f
        val absentSweep = summary.absent / total * 360f
        val gap = if (summary.present > 0 && summary.absent > 0) 3f else 0f
        var start = -90f
        if (summary.present > 0) {
            arcPaint.color = hex("#F2C94C")
            canvas.drawArc(arcRect, start + gap / 2f, (presentSweep - gap).coerceAtLeast(1f), false, arcPaint)
            start += presentSweep
        }
        if (summary.absent > 0) {
            arcPaint.color = hex("#C084FC")
            canvas.drawArc(arcRect, start + gap / 2f, (absentSweep - gap).coerceAtLeast(1f), false, arcPaint)
        }

        // percentage in the middle of the ring
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifBold
            textAlign = Paint.Align.CENTER
            textSize = diameter * 0.27f
            color = hex("#FFF7D6")
        }
        val centerBaseline = centerY - (centerPaint.fontMetrics.ascent + centerPaint.fontMetrics.descent) / 2f
        canvas.drawText("$presentPct%", chartLeft + diameter / 2f, centerBaseline, centerPaint)

        x += diameter + 12f * unit

        // ---- text block ----
        val textWidth = w - x - padding
        val tall = heightDp >= 56f

        val presentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifBold
            textSize = minOf(h * 0.26f, 17f * unit)
            color = hex("#F9E7A6")
        }
        val absentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = presentPaint.textSize * 0.8f
            color = hex("#D8B4FE")
        }
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = presentPaint.textSize * 0.5f
            letterSpacing = 0.18f
            color = hex("#A78BFA")
        }

        val stale = WidgetFreshness.isStale(summary.lastSyncedMillis)
        val presentText = "Present $presentPct%"
        val absentText = if (stale) "Absent $absentPct% (old)" else "Absent $absentPct%"
        val widest = maxOf(presentPaint.measureText(presentText), absentPaint.measureText(absentText))
        if (widest > textWidth && textWidth > 0f) {
            val factor = (textWidth / widest).coerceAtLeast(0.6f)
            presentPaint.textSize *= factor
            absentPaint.textSize *= factor
            captionPaint.textSize *= factor
        }

        val lines = mutableListOf<Pair<String, Paint>>()
        if (tall) lines.add((if (stale) "ATTENDANCE · OLD DATA" else "ATTENDANCE") to captionPaint)
        lines.add(presentText to presentPaint)
        lines.add(absentText to absentPaint)

        val lineHeights = lines.map { it.second.textSize * 1.22f }
        var y = centerY - lineHeights.sum() / 2f
        lines.forEachIndexed { index, line ->
            val baseline = y + lineHeights[index] * 0.82f
            canvas.drawText(line.first, x, baseline, line.second)
            y += lineHeights[index]
        }

        return bitmap
    }
}
