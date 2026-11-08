package com.example.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.MarkStatus
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Bridges the first release's SharedPreferences format into Room without deleting the rollback copy. */
object AcademicRoomRepository {
    private const val PREFS = "clg1_academic_local"
    private const val MIGRATION_KEY = "room_migration_complete_v1"
    private val workspaceMutex = Mutex()

    suspend fun migrateLegacyPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString("academic_records_v1", null)
        if (json.isNullOrBlank()) {
            prefs.edit().putBoolean(MIGRATION_KEY, true).apply()
            return
        }
        val profileMetadataRaw = prefs.getString("academic_profile_v1", "{}") ?: "{}"
        val currentHash = sha256("$json|$profileMetadataRaw")
        if (prefs.getString("room_mirror_hash", null) != currentHash) {
            persistFromLegacyJson(
                context = context,
                recordsJson = json,
                college = prefs.getString("college", "") ?: "",
                term = prefs.getString("term", "Semester 1") ?: "Semester 1",
                academicYear = prefs.getString("academic_year", "") ?: ""
            )
        }
        // Legacy preferences remain as a recovery copy. The hash tracks the Room mirror version.
        prefs.edit().putBoolean(MIGRATION_KEY, true).apply()
    }

    suspend fun persistFromLegacyJson(
        context: Context,
        recordsJson: String,
        college: String,
        term: String,
        academicYear: String = ""
    ) = workspaceMutex.withLock {
        require(recordsJson.length <= 4 * 1024 * 1024) { "Academic workspace is too large" }
        val root = JSONObject(recordsJson)
        require(root.optInt("schema_version", 1) == 1) { "Unsupported academic workspace version" }
        val now = System.currentTimeMillis()
        val safeTerm = term.take(100).ifBlank { "Current term" }
        val dao = Clg1Database.get(context).academicDao()
        val profileMetadataRaw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("academic_profile_v1", "{}") ?: "{}"
        val profileMetadata = runCatching { JSONObject(profileMetadataRaw) }.getOrDefault(JSONObject())
        validateProfileMetadata(profileMetadata)
        val configuredPeriods = profileMetadata.optJSONArray("periods")
        val periods = if (configuredPeriods != null && configuredPeriods.length() in 1..40) {
            (0 until configuredPeriods.length()).map { index ->
                val item = configuredPeriods.getJSONObject(index)
                AcademicPeriodEntity(
                    id = item.optString("id").ifBlank { "period-$index" },
                    name = item.optString("name").take(100).ifBlank { "Period ${index + 1}" },
                    orderIndex = item.optInt("order", index),
                    startsAtMillis = if (item.isNull("starts_at")) null else item.optLong("starts_at"),
                    endsAtMillis = if (item.isNull("ends_at")) null else item.optLong("ends_at"),
                    examAtMillis = if (item.isNull("exam_at")) null else item.optLong("exam_at")
                )
            }
        } else listOf(AcademicPeriodEntity(id = "legacy-${safeTerm.hashCode()}", name = safeTerm, orderIndex = 0, startsAtMillis = null, endsAtMillis = null, examAtMillis = null))
        require(periods.map { it.id }.distinct().size == periods.size) { "Duplicate academic period IDs" }
        require(periods.map { it.name.lowercase() }.distinct().size == periods.size) { "Duplicate academic period names" }
        val currentPeriod = periods.firstOrNull { it.name == safeTerm } ?: periods.first()
        val termId = currentPeriod.id
        val subjectArray = root.optJSONArray("subjects") ?: JSONArray()
        require(subjectArray.length() <= 500) { "Too many subjects in academic workspace" }
        val subjects = mutableListOf<AcademicSubjectEntity>()
        val marks = mutableListOf<AssessmentMarkEntity>()
        for (i in 0 until subjectArray.length()) {
            val subject = subjectArray.getJSONObject(i)
            val subjectId = subject.optString("id").ifBlank { UUID.randomUUID().toString() }
            val credits = subject.optDouble("credits", 0.0)
            require(credits.isFinite() && credits in 0.0..100.0) { "Invalid subject credits" }
            require(subject.optString("name").isNotBlank()) { "Subject name is required" }
            val subjectPass = if (subject.isNull("subject_pass_percent")) null else subject.optDouble("subject_pass_percent")
            require(subjectPass == null || (subjectPass.isFinite() && subjectPass in 0.0..100.0)) { "Invalid subject pass threshold" }
            subjects += AcademicSubjectEntity(
                id = subjectId,
                code = subject.optString("code").take(40),
                name = subject.optString("name").take(120),
                credits = credits,
                termId = termId,
                subjectPassPercent = subjectPass
            )
            val assessmentArray = subject.optJSONArray("assessments") ?: JSONArray()
            require(assessmentArray.length() <= 100) { "Too many components for a subject" }
            for (j in 0 until assessmentArray.length()) {
                val mark = assessmentArray.getJSONObject(j)
                val maximum = mark.optDouble("maximum", 100.0)
                val weight = mark.optDouble("weight", 100.0)
                val obtained = if (mark.isNull("obtained")) null else mark.optDouble("obtained")
                val minimumPass = if (mark.isNull("minimum_pass")) null else mark.optDouble("minimum_pass")
                val status = mark.optString("status", if (obtained == null) MarkStatus.NOT_CONDUCTED.name else MarkStatus.RECORDED.name)
                require(maximum.isFinite() && maximum > 0.0 && weight.isFinite() && weight in 0.0..100.0) { "Invalid assessment maximum or weight" }
                require(obtained == null || (obtained.isFinite() && obtained in 0.0..maximum)) { "Invalid recorded marks" }
                require(minimumPass == null || (minimumPass.isFinite() && minimumPass in 0.0..100.0)) { "Invalid assessment pass threshold" }
                require(MarkStatus.values().any { it.name == status }) { "Invalid assessment status" }
                require((status == MarkStatus.RECORDED.name) == (obtained != null)) { "Recorded marks and assessment status do not match" }
                marks += AssessmentMarkEntity(
                    id = "$subjectId-$j", subjectId = subjectId,
                    name = mark.optString("name", "Assessment").take(100), obtained = obtained,
                    maximum = maximum, weightPercent = weight,
                    status = status,
                    minimumPassPercent = minimumPass,
                    category = mark.optString("category", "assessment").take(40),
                    dateEpochMillis = if (mark.isNull("date")) null else mark.optLong("date"),
                    sequence = j
                )
            }
            val configuredWeight = marks.filter { it.subjectId == subjectId && it.status != MarkStatus.EXEMPTED.name }.sumOf { it.weightPercent }
            require(configuredWeight <= 100.000001) { "Assessment weights exceed 100% for ${subject.optString("name")}" }
        }
        require(subjects.map { it.id }.distinct().size == subjects.size) { "Duplicate subject IDs" }
        require(subjects.map { it.name.lowercase() }.distinct().size == subjects.size) { "Duplicate subject names" }
        val nonBlankCodes = subjects.map { it.code.lowercase() }.filter { it.isNotBlank() }
        require(nonBlankCodes.distinct().size == nonBlankCodes.size) { "Duplicate subject codes" }
        val examArray = root.optJSONArray("exams") ?: JSONArray()
        require(examArray.length() <= 2000) { "Too many exams in academic workspace" }
        val exams = (0 until examArray.length()).map { index ->
            val exam = examArray.getJSONObject(index)
            require(exam.optString("title").isNotBlank() && exam.optString("subject").isNotBlank() && exam.optLong("date") > 0L) { "Invalid exam event" }
            AcademicExamEntity(
                id = "legacy-exam-$index-${exam.optLong("date")}",
                title = exam.optString("title").take(120), subject = exam.optString("subject").take(120),
                dateMillis = exam.optLong("date"), notes = exam.optString("notes").take(1000), termId = termId
            )
        }
        val profile = AcademicProfileEntity(
            college = college.take(160), termName = safeTerm, academicYear = academicYear.take(40),
            configurationJson = JSONObject().put("schema_version", 1)
                .put("combination_method", root.optString("combination_method", "WEIGHTED_AVERAGE"))
                .put("missing_mark_policy", root.optString("missing_mark_policy", "EXCLUDE_AND_RENORMALIZE"))
                .put("best_n", root.optInt("best_n", 1))
                .put("passing_percent", root.optDouble("passing_percent", 40.0))
                .put("grade_bands", root.optJSONArray("grade_bands") ?: JSONArray())
                .put("university", profileMetadata.optString("university"))
                .put("program", profileMetadata.optString("program"))
                .put("department", profileMetadata.optString("department"))
                .put("regulation", profileMetadata.optString("regulation"))
                .put("academic_year", academicYear.take(40))
                .put("calendar", profileMetadata.optString("calendar", "SEMESTER"))
                .put("periods", profileMetadata.optJSONArray("periods") ?: JSONArray())
                .put("assessment_components", profileMetadata.optJSONArray("assessment_components") ?: JSONArray())
                .put("attendance_affects_marks", profileMetadata.optBoolean("attendance_affects_marks", false))
                .put("attendance_marks_maximum", if (profileMetadata.isNull("attendance_marks_maximum")) JSONObject.NULL else profileMetadata.optDouble("attendance_marks_maximum")).toString(),
            updatedAtMillis = now
        )
        dao.replaceWorkspace(
            profile = profile,
            periods = periods,
            subjects = subjects,
            marks = marks,
            exams = exams,
            audit = AcademicAuditEntity(UUID.randomUUID().toString(), "workspace", "LEGACY_MIGRATION_OR_UPDATE", now, "Academic records synchronized to Room")
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("room_mirror_hash", sha256("$recordsJson|$profileMetadataRaw")).apply()
    }

    fun validateProfileMetadata(metadata: JSONObject) {
        val periods = metadata.optJSONArray("periods")
        if (periods != null) {
            require(periods.length() <= 40) { "Too many academic periods" }
            val ids = mutableSetOf<String>()
            val names = mutableSetOf<String>()
            for (index in 0 until periods.length()) {
                val period = periods.getJSONObject(index)
                val id = period.optString("id").take(80)
                val name = period.optString("name").trim().take(100)
                require(id.isNotBlank() && ids.add(id)) { "Academic period IDs must be present and unique" }
                require(name.isNotBlank() && names.add(name.lowercase())) { "Academic period names must be present and unique" }
                val start = if (period.isNull("starts_at")) null else period.optLong("starts_at")
                val end = if (period.isNull("ends_at")) null else period.optLong("ends_at")
                require(start == null || end == null || end >= start) { "Academic period ends before it starts" }
            }
        }
        val components = metadata.optJSONArray("assessment_components")
        if (components != null) {
            require(components.length() <= 100) { "Too many assessment components" }
            val ids = mutableSetOf<String>()
            val names = mutableSetOf<String>()
            var totalWeight = 0.0
            for (index in 0 until components.length()) {
                val component = components.getJSONObject(index)
                val id = component.optString("id").take(80)
                val name = component.optString("name").trim().take(100)
                val category = component.optString("category").trim()
                val maximum = component.optDouble("maximum", Double.NaN)
                val weight = component.optDouble("weight", Double.NaN)
                val minimumPass = if (component.isNull("minimum_pass")) null else component.optDouble("minimum_pass")
                require(id.isNotBlank() && ids.add(id)) { "Assessment component IDs must be unique" }
                require(name.isNotBlank() && names.add(name.lowercase())) { "Assessment component names must be unique" }
                require(category.isNotBlank() && maximum.isFinite() && maximum > 0.0 && weight.isFinite() && weight in 0.0..100.0) { "Invalid assessment component settings" }
                require(minimumPass == null || (minimumPass.isFinite() && minimumPass in 0.0..100.0)) { "Invalid component pass threshold" }
                totalWeight += weight
            }
            require(components.length() == 0 || kotlin.math.abs(totalWeight - 100.0) <= 0.000001) { "Assessment component weights must total 100%" }
        }
    }

    suspend fun deleteAll(context: Context) = workspaceMutex.withLock {
        Clg1Database.get(context).academicDao().clearWorkspace()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("room_mirror_hash").apply()
    }

    suspend fun historyToJson(context: Context): JSONArray {
        val snapshots = Clg1Database.get(context).academicDao().getSnapshots()
        return JSONArray().apply {
            snapshots.forEach { item ->
                put(JSONObject().put("id", item.id).put("academic_year", item.academicYear)
                    .put("term_name", item.termName).put("captured_at", item.capturedAtMillis)
                    .put("subject_count", item.subjectCount).put("completed_subject_count", item.completedSubjectCount)
                    .put("average_percentage", item.averagePercentage ?: JSONObject.NULL)
                    .put("sgpa", item.sgpa ?: JSONObject.NULL).put("credits", item.credits)
                    .put("subject_summaries", item.subjectSummariesJson))
            }
        }
    }

    fun parseHistoryJson(values: JSONArray): List<AcademicSnapshotEntity> {
        require(values.length() <= 500) { "Backup contains too many historical snapshots" }
        return (0 until values.length()).map { index ->
            val item = values.getJSONObject(index)
            val average = if (item.isNull("average_percentage")) null else item.optDouble("average_percentage")
            val sgpa = if (item.isNull("sgpa")) null else item.optDouble("sgpa")
            require(average == null || (average.isFinite() && average in 0.0..100.0)) { "Invalid historical average" }
            require(sgpa == null || (sgpa.isFinite() && sgpa >= 0.0)) { "Invalid historical SGPA" }
            val summaries = item.optString("subject_summaries", "[]")
            require(summaries.length <= 512 * 1024) { "Historical summary is too large" }
            val summaryArray = JSONArray(summaries)
            require(summaryArray.length() <= 500) { "Historical snapshot contains too many subjects" }
            AcademicSnapshotEntity(
                id = item.optString("id").take(80).ifBlank { UUID.randomUUID().toString() },
                academicYear = item.optString("academic_year").take(40),
                termName = item.optString("term_name").take(100),
                capturedAtMillis = item.optLong("captured_at"),
                subjectCount = item.optInt("subject_count").coerceIn(0, 500),
                completedSubjectCount = item.optInt("completed_subject_count").coerceIn(0, 500),
                averagePercentage = average, sgpa = sgpa,
                credits = item.optDouble("credits", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                subjectSummariesJson = summaries
            )
        }
    }

    suspend fun restoreHistory(context: Context, values: JSONArray) = workspaceMutex.withLock {
        val snapshots = parseHistoryJson(values)
        Clg1Database.get(context).academicDao().replaceSnapshots(snapshots)
    }

    private fun sha256(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
