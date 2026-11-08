package com.example.data.analytics

import android.content.Context
import com.example.data.local.AcademicSnapshotEntity
import com.example.data.local.Clg1Database
import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.domain.AcademicRulesEngine
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object AcademicHistoryService {
    suspend fun archiveTerm(
        context: Context,
        academicYear: String,
        termName: String,
        subjects: List<AcademicSubject>,
        rules: AcademicRules
    ): AcademicSnapshotEntity {
        require(academicYear.isNotBlank()) { "Set the academic year in the setup wizard before archiving history" }
        require(subjects.isNotEmpty()) { "Add subjects before archiving a term" }
        val results = subjects.map { it to AcademicRulesEngine.calculate(it, rules) }
        val completed = results.count { !it.second.provisional && it.second.percentage != null }
        val percentages = results.mapNotNull { it.second.percentage }
        val avg = percentages.takeIf { it.isNotEmpty() }?.average()
        val sgpa = AcademicRulesEngine.sgpa(subjects, rules)
        val credits = subjects.filter { it.credits.isFinite() && it.credits > 0.0 }.sumOf { it.credits }
        val data = JSONArray()
        results.forEach { (subject, result) ->
            data.put(JSONObject()
                .put("id", subject.id).put("code", subject.code).put("name", subject.name)
                .put("credits", subject.credits).put("percentage", result.percentage ?: JSONObject.NULL)
                .put("grade", result.grade ?: JSONObject.NULL).put("grade_point", result.gradePoint ?: JSONObject.NULL)
                .put("passed", result.passed ?: JSONObject.NULL).put("provisional", result.provisional)
                .put("explanation", result.explanation)
                .put("assessment_count", subject.assessments.size)
                .put("recorded_count", subject.assessments.count { it.obtained != null })
                .put("assessments", JSONArray().apply {
                    subject.assessments.forEach { mark ->
                        put(JSONObject()
                            .put("name", mark.name)
                            .put("category", mark.category)
                            .put("obtained", mark.obtained ?: JSONObject.NULL)
                            .put("maximum", mark.maximum)
                            .put("weight_percent", mark.weightPercent)
                            .put("normalized_percent", mark.obtained?.let { if (mark.maximum.isFinite() && mark.maximum > 0.0) it / mark.maximum * 100.0 else JSONObject.NULL } ?: JSONObject.NULL)
                            .put("status", mark.status.name))
                    }
                }))
        }
        val snapshot = AcademicSnapshotEntity(
            id = UUID.nameUUIDFromBytes("$academicYear|$termName".toByteArray(Charsets.UTF_8)).toString(), academicYear = academicYear.take(40),
            termName = termName.take(100), capturedAtMillis = System.currentTimeMillis(),
            subjectCount = subjects.size, completedSubjectCount = completed,
            averagePercentage = avg, sgpa = sgpa, credits = credits,
            subjectSummariesJson = data.toString()
        )
        Clg1Database.get(context).academicDao().upsertSnapshot(snapshot)
        return snapshot
    }
}
