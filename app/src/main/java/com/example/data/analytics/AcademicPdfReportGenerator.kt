package com.example.data.analytics

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.AcademicSnapshotEntity
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates a local, user-shared PDF report. The report is not uploaded by this class. */
object AcademicPdfReportGenerator {
    fun generate(context: Context, snapshotsNewestFirst: List<AcademicSnapshotEntity>, title: String = "Clg 1% Academic Report"): File {
        require(snapshotsNewestFirst.isNotEmpty()) { "Archive at least one term before creating a historical report" }
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; alpha = 170 }
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 0.7f; alpha = 90 }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = 56f
            page!!.canvas.drawText(title, margin, y, titlePaint)
            y += 20f
            page!!.canvas.drawText("Generated ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())} · Local academic data", margin, y, mutedPaint)
            y += 28f
        }
        fun finishPage() {
            page?.let {
                it.canvas.drawLine(margin, pageHeight - 34f, pageWidth - margin, pageHeight - 34f, pagePaint)
                it.canvas.drawText("Clg 1% · Personal tracking only · Page $pageNumber", margin, pageHeight - 20f, mutedPaint)
                document.finishPage(it)
            }
            page = null
        }
        fun ensureSpace(lines: Int = 1) { if (y + lines * 17f > pageHeight - 48f) { finishPage(); startPage() } }
        fun line(text: String, paint: Paint = bodyPaint) {
            val maxWidth = pageWidth - margin * 2
            val words = text.split(Regex("\\s+"))
            var current = StringBuilder()
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (current.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                    ensureSpace(); page!!.canvas.drawText(current.toString(), margin, y, paint); y += 16f
                    current = StringBuilder(word)
                } else current = StringBuilder(candidate)
            }
            if (current.isNotEmpty()) { ensureSpace(); page!!.canvas.drawText(current.toString(), margin, y, paint); y += 16f }
        }
        startPage()
        val summary = HistoricalAcademicAnalytics.summarize(snapshotsNewestFirst)
        line("Historical overview", headingPaint)
        line("Archived terms: ${summary.snapshots}")
        line("Latest term average: ${summary.latestAverage?.let { "%.2f%%".format(it) } ?: "Not available"}")
        line("Latest SGPA: ${summary.latestSgpa?.let { "%.2f".format(it) } ?: "Not enough verified data"}")
        line("Cumulative CGPA (archived terms): ${summary.overallCgpa?.let { "%.2f".format(it) } ?: "Not enough verified term credits"}")
        line("Trend: ${summary.trendLabel}")
        line("Historical data points: ${summary.historicalDataPoints}")
        if (summary.gradeDistribution.isNotEmpty()) line("Latest term grade distribution: ${summary.gradeDistribution.entries.joinToString { "${it.key} ${it.value}" }}")
        if (summary.historicalGradeDistribution.isNotEmpty()) line("All archived terms grade distribution: ${summary.historicalGradeDistribution.entries.joinToString { "${it.key} ${it.value}" }}")
        summary.riskFlags.forEach { line("• $it") }
        if (summary.subjectPerformance.isNotEmpty()) {
            y += 6f
            line("Latest subject performance", headingPaint)
            summary.subjectPerformance.forEach { (name, percentage) -> line("$name: ${"%.2f%%".format(percentage)}") }
        }
        if (summary.subjectTrendDeltas.isNotEmpty()) {
            y += 6f
            line("Subject trends vs previous archived period", headingPaint)
            summary.subjectTrendDeltas.forEach { (code, delta) -> line("$code: ${if (delta >= 0) "+" else ""}${"%.2f".format(delta)} percentage points") }
        }
        if (summary.assessmentCategoryAverages.isNotEmpty()) {
            y += 6f
            line("Assessment category averages", headingPaint)
            summary.assessmentCategoryAverages.forEach { (category, percentage) -> line("$category: ${"%.2f%%".format(percentage)}") }
        }
        if (summary.historicalAssessmentCategoryAverages.isNotEmpty()) {
            y += 6f
            line("Historical assessment category averages (all archived terms)", headingPaint)
            summary.historicalAssessmentCategoryAverages.forEach { (category, percentage) -> line("$category: ${"%.2f%%".format(percentage)}") }
        }
        y += 8f
        line("Term-by-term results", headingPaint)
        summary.termTrends.forEach { point ->
            line("${point.academicYear} · ${point.termName}: average ${point.averagePercentage?.let { "%.2f%%".format(it) } ?: "—"}, SGPA ${point.sgpa?.let { "%.2f".format(it) } ?: "—"}, credits ${"%.1f".format(point.credits)}")
        }
        snapshotsNewestFirst.sortedBy { it.capturedAtMillis }.forEach { snapshot ->
            ensureSpace(5)
            line("${snapshot.termName}  |  ${snapshot.academicYear.ifBlank { "Academic year not set" }}", headingPaint)
            line("Average: ${snapshot.averagePercentage?.let { "%.2f%%".format(it) } ?: "—"}  ·  SGPA: ${snapshot.sgpa?.let { "%.2f".format(it) } ?: "—"}")
            line("Completed subjects: ${snapshot.completedSubjectCount}/${snapshot.subjectCount}  ·  Credits: ${"%.1f".format(snapshot.credits)}")
            val subjects = runCatching { JSONArray(snapshot.subjectSummariesJson) }.getOrDefault(JSONArray())
            for (i in 0 until subjects.length()) {
                val item = subjects.optJSONObject(i) ?: continue
                val percent = if (item.isNull("percentage")) "pending" else "%.2f%%".format(item.optDouble("percentage"))
                line("  ${item.optString("code")} ${item.optString("name")}: $percent · ${item.optString("grade", "—")}${if (item.optBoolean("provisional")) " (provisional)" else ""}")
                val categoryParts = listOf("Internal" to "internal_percent", "Practical" to "practical_percent", "External" to "external_percent")
                    .filter { (_, key) -> !item.isNull(key) }
                    .joinToString(" · ") { (label, key) -> "$label ${"%.1f".format(item.optDouble(key))}%" }
                if (categoryParts.isNotBlank()) line("    $categoryParts")
            }
            y += 5f
        }
        finishPage()
        val dir = File(context.cacheDir, "academic_exports").apply { mkdirs() }
        val file = File(dir, "clg1-historical-report-${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
