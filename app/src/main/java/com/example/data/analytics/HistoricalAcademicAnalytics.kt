package com.example.data.analytics

import com.example.data.local.AcademicSnapshotEntity
import com.example.data.model.AcademicTermResult
import com.example.domain.AcademicRulesEngine
import org.json.JSONArray
import org.json.JSONObject

/** Deterministic historical summaries; no AI confidence or unsupported forecasting is implied. */
data class AcademicTermTrendPoint(
    val academicYear: String,
    val termName: String,
    val capturedAtMillis: Long,
    val averagePercentage: Double?,
    val sgpa: Double?,
    val credits: Double
)

data class HistoricalAnalyticsSummary(
    val snapshots: Int,
    val latestAverage: Double?,
    val previousAverage: Double?,
    val averageDelta: Double?,
    val latestSgpa: Double?,
    val previousSgpa: Double?,
    val sgpaDelta: Double?,
    val trendLabel: String,
    val riskFlags: List<String>,
    val gradeDistribution: Map<String, Int> = emptyMap(),
    val subjectPerformance: List<Pair<String, Double>> = emptyList(),
    val assessmentCategoryAverages: Map<String, Double> = emptyMap(),
    val historicalDataPoints: Int = 0,
    val subjectTrendDeltas: Map<String, Double> = emptyMap(),
    val overallCgpa: Double? = null,
    val termTrends: List<AcademicTermTrendPoint> = emptyList(),
    val historicalGradeDistribution: Map<String, Int> = emptyMap(),
    val historicalAssessmentCategoryAverages: Map<String, Double> = emptyMap()
)

object HistoricalAcademicAnalytics {
    fun summarize(snapshotsNewestFirst: List<AcademicSnapshotEntity>): HistoricalAnalyticsSummary {
        val ordered = snapshotsNewestFirst.sortedByDescending { it.capturedAtMillis }
        val latest = ordered.getOrNull(0)
        val previous = ordered.getOrNull(1)
        val latestAverage = latest?.averagePercentage
        val previousAverage = previous?.averagePercentage
        val averageDelta = if (latestAverage != null && previousAverage != null) latestAverage - previousAverage else null
        val latestSgpa = latest?.sgpa
        val previousSgpa = previous?.sgpa
        val sgpaDelta = if (latestSgpa != null && previousSgpa != null) latestSgpa - previousSgpa else null
        val flags = buildList {
            if (latest == null) add("No historical term has been archived yet.")
            else {
                if (latest.completedSubjectCount < latest.subjectCount) add("Latest term has ${latest.subjectCount - latest.completedSubjectCount} incomplete or provisional subject(s).")
                val subjects = runCatching { JSONObject("{\"subjects\":${latest.subjectSummariesJson}}")
                    .optJSONArray("subjects") }.getOrNull()
                if (subjects != null) for (i in 0 until subjects.length()) {
                    val item = subjects.optJSONObject(i) ?: continue
                    if (item.optBoolean("passed") == false) add("${item.optString("name", "A subject")}: configured pass threshold not met.")
                    if (item.optBoolean("provisional")) add("${item.optString("name", "A subject")}: result is provisional because marks are incomplete.")
                }
            }
        }
        val latestSubjects = latest?.let { runCatching { JSONArray(it.subjectSummariesJson) }.getOrNull() }
        val previousSubjects = previous?.let { runCatching { JSONArray(it.subjectSummariesJson) }.getOrNull() }
        fun subjectPercentages(array: JSONArray?): Map<String, Double> = buildMap {
            if (array != null) for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                if (!item.isNull("percentage")) put(item.optString("code").ifBlank { item.optString("name", "Subject") }, item.optDouble("percentage"))
            }
        }
        val latestSubjectPercentages = subjectPercentages(latestSubjects)
        val previousSubjectPercentages = subjectPercentages(previousSubjects)
        val subjectTrendDeltas = latestSubjectPercentages.mapNotNull { (name, value) ->
            previousSubjectPercentages[name]?.let { name to (value - it) }
        }.toMap()
        val subjectPerformance = buildList<Pair<String, Double>> {
            if (latestSubjects != null) for (i in 0 until latestSubjects.length()) {
                val item = latestSubjects.optJSONObject(i) ?: continue
                if (!item.isNull("percentage")) add(item.optString("name", "Subject") to item.optDouble("percentage"))
            }
        }.sortedByDescending { it.second }
        val gradeDistribution = buildMap<String, Int> {
            if (latestSubjects != null) for (i in 0 until latestSubjects.length()) {
                val item = latestSubjects.optJSONObject(i) ?: continue
                if (!item.isNull("grade")) {
                    val grade = item.optString("grade")
                    put(grade, (get(grade) ?: 0) + 1)
                }
            }
        }
        fun categoryValuesFor(source: List<AcademicSnapshotEntity>): Map<String, List<Double>> {
            val values = linkedMapOf<String, MutableList<Double>>()
            source.forEach { snapshot ->
                val subjects = runCatching { JSONArray(snapshot.subjectSummariesJson) }.getOrNull() ?: return@forEach
                for (i in 0 until subjects.length()) {
                    val subject = subjects.optJSONObject(i) ?: continue
                    val assessments = subject.optJSONArray("assessments") ?: continue
                    for (j in 0 until assessments.length()) {
                        val assessment = assessments.optJSONObject(j) ?: continue
                        if (assessment.isNull("normalized_percent")) continue
                        val value = assessment.optDouble("normalized_percent")
                        if (!value.isFinite() || value !in 0.0..100.0) continue
                        val category = assessment.optString("category", "other").ifBlank { "other" }
                        values.getOrPut(category) { mutableListOf() }.add(value)
                    }
                }
            }
            return values
        }
        val latestCategoryValues = categoryValuesFor(listOfNotNull(latest))
        val categoryAverages = latestCategoryValues.mapValues { (_, values) -> values.average() }
        val historicalCategoryAverages = categoryValuesFor(ordered).mapValues { (_, values) -> values.average() }
        val historicalGradeDistribution = buildMap<String, Int> {
            ordered.forEach { snapshot ->
                val subjects = runCatching { JSONArray(snapshot.subjectSummariesJson) }.getOrNull() ?: return@forEach
                for (i in 0 until subjects.length()) {
                    val item = subjects.optJSONObject(i) ?: continue
                    if (item.isNull("grade")) continue
                    val grade = item.optString("grade").trim()
                    if (grade.isNotEmpty()) put(grade, (get(grade) ?: 0) + 1)
                }
            }
        }
        val termTrends = ordered.asReversed().map { snapshot ->
            AcademicTermTrendPoint(snapshot.academicYear, snapshot.termName, snapshot.capturedAtMillis,
                snapshot.averagePercentage, snapshot.sgpa, snapshot.credits)
        }
        val historicalDataPoints = ordered.count { it.averagePercentage != null }
        val overallCgpa = AcademicRulesEngine.cgpa(ordered.mapNotNull { snapshot ->
            if (snapshot.sgpa != null && snapshot.credits > 0.0) AcademicTermResult(snapshot.termName, snapshot.credits, snapshot.sgpa) else null
        })
        val trend = when {
            ordered.size < 2 -> "Not enough historical data yet"
            averageDelta == null -> "Percentage trend unavailable"
            averageDelta > 0.25 -> "Improving by ${"%.2f".format(averageDelta)} percentage points"
            averageDelta < -0.25 -> "Declining by ${"%.2f".format(-averageDelta)} percentage points"
            else -> "Broadly stable across the last two archived terms"
        }
        return HistoricalAnalyticsSummary(ordered.size, latestAverage, previousAverage, averageDelta,
            latestSgpa, previousSgpa, sgpaDelta, trend, flags.distinct(), gradeDistribution,
            subjectPerformance, categoryAverages, historicalDataPoints, subjectTrendDeltas, overallCgpa,
            termTrends, historicalGradeDistribution, historicalCategoryAverages)
    }
}
