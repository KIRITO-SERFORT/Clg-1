package com.example.data.template

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale

/** Versioned local academic-profile codec; intentionally has no fields for marks, identity or ERP secrets. */
object AcademicProfileCodec {
    const val VERSION = 1

    fun encode(profile: AcademicProfile): String {
        val root = JSONObject().put("schema_version", VERSION)
            .put("institution", profile.institution).put("university", profile.university)
            .put("program", profile.program).put("department", profile.department)
            .put("regulation", profile.regulation).put("academic_year", profile.academicYear)
            .put("calendar_type", profile.calendarType.name)
        val periods = JSONArray()
        profile.periods.forEach { p -> periods.put(JSONObject().put("id", p.id).put("name", p.name)
            .put("order", p.order).put("starts_at", p.startsAtEpochMillis ?: JSONObject.NULL)
            .put("ends_at", p.endsAtEpochMillis ?: JSONObject.NULL).put("exam_at", p.examAtEpochMillis ?: JSONObject.NULL)) }
        root.put("periods", periods)
        val scheme = JSONObject().put("method", profile.defaultScheme.combinationMethod.name)
            .put("missing_policy", profile.defaultScheme.missingMarkPolicy.name)
            .put("best_n", profile.defaultScheme.bestN).put("require_total_100", profile.defaultScheme.requireWeightsToTotal100)
        val components = JSONArray()
        profile.defaultScheme.components.forEach { c -> components.put(JSONObject().put("id", c.id).put("name", c.name)
            .put("category", c.category).put("maximum", c.maximumMarks).put("weight", c.weightPercent)
            .put("minimum_pass", c.minimumPassPercent ?: JSONObject.NULL).put("required", c.required)) }
        scheme.put("components", components); root.put("assessment_scheme", scheme)
        val grading = JSONObject().put("passing_percent", profile.grading.passingPercent)
            .put("decimal_places", profile.grading.decimalPlaces).put("boundary_inclusive", profile.grading.boundaryInclusive)
            .put("relative_grading", profile.grading.relativeGradingEnabled)
            .put("attendance_affects_marks", profile.grading.attendanceAffectsMarks)
            .put("attendance_marks_maximum", profile.grading.attendanceMarksMaximum ?: JSONObject.NULL)
        val bands = JSONArray()
        profile.grading.bands.forEach { b -> bands.put(JSONObject().put("minimum", b.minimumPercent)
            .put("label", b.label).put("point", b.gradePoint).put("inclusive", b.inclusive)) }
        grading.put("bands", bands); root.put("grading", grading)
        return root.toString()
    }

    fun decode(json: String, validateProfile: Boolean = true): AcademicProfile {
        require(json.length in 1..256 * 1024) { "Academic setup is empty or too large" }
        val root = JSONObject(json)
        require(root.optInt("schema_version", -1) == VERSION) { "Unsupported academic setup version" }
        val calendar = runCatching { AcademicCalendarType.valueOf(root.optString("calendar_type")) }
            .getOrDefault(AcademicCalendarType.SEMESTER)
        val periodJson = root.optJSONArray("periods") ?: JSONArray()
        require(periodJson.length() <= 40) { "Too many academic periods" }
        val periods = (0 until periodJson.length()).map { i ->
            val p = periodJson.getJSONObject(i)
            AcademicPeriod(p.optString("id"), p.optString("name"), p.optInt("order"),
                p.longOrNull("starts_at"), p.longOrNull("ends_at"), p.longOrNull("exam_at"))
        }
        val schemeJson = root.optJSONObject("assessment_scheme") ?: JSONObject()
        val componentJson = schemeJson.optJSONArray("components") ?: JSONArray()
        require(componentJson.length() <= 100) { "Too many assessment components" }
        val components = (0 until componentJson.length()).map { i ->
            val c = componentJson.getJSONObject(i)
            AssessmentComponentDefinition(c.optString("id"), c.optString("name"), c.optString("category", "assessment"),
                c.optDouble("maximum", Double.NaN), c.optDouble("weight", Double.NaN), c.doubleOrNull("minimum_pass"), c.optBoolean("required", true))
        }
        val method = runCatching { CombinationMethod.valueOf(schemeJson.optString("method")) }.getOrDefault(CombinationMethod.WEIGHTED_AVERAGE)
        val missing = runCatching { MissingMarkPolicy.valueOf(schemeJson.optString("missing_policy")) }.getOrDefault(MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE)
        val assessmentScheme = AssessmentScheme(components, method, missing, schemeJson.optInt("best_n", 1), schemeJson.optBoolean("require_total_100", true))
        val gradingJson = root.optJSONObject("grading") ?: JSONObject()
        val bandJson = gradingJson.optJSONArray("bands") ?: JSONArray()
        require(bandJson.length() <= 30) { "Too many grade bands" }
        val bands = (0 until bandJson.length()).map { i ->
            val b = bandJson.getJSONObject(i)
            GradeBand(b.optDouble("minimum", Double.NaN), b.optString("label"), b.optDouble("point", Double.NaN), b.optBoolean("inclusive", true))
        }
        val grading = GradingScheme(bands, gradingJson.optDouble("passing_percent", 40.0),
            gradingJson.optInt("decimal_places", 2), gradingJson.optBoolean("boundary_inclusive", true),
            gradingJson.optBoolean("relative_grading", false), gradingJson.optBoolean("attendance_affects_marks", false),
            gradingJson.doubleOrNull("attendance_marks_maximum"))
        val profile = AcademicProfile(VERSION, root.optString("institution"), root.optString("university"),
            root.optString("program"), root.optString("department"), root.optString("regulation"),
            root.optString("academic_year"), calendar, periods, assessmentScheme, grading)
        if (validateProfile) {
            val errors = com.example.domain.AcademicSetupValidator.validate(profile)
            require(errors.isEmpty()) { errors.joinToString("; ") }
        }
        return profile
    }

    fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        return runCatching {
            val position = ParsePosition(0)
            val parsed = format.parse(value, position)
            parsed?.takeIf { position.errorIndex < 0 && position.index == value.length }?.time
        }.getOrNull()
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else optLong(key)
    private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key, Double.NaN)
}
