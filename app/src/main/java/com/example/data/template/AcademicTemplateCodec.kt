package com.example.data.template

import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.data.model.AssessmentMark
import com.example.data.model.MarkStatus
import com.example.data.model.CollegeTemplate
import com.example.data.model.CombinationMethod
import com.example.data.model.GradeBand
import com.example.data.model.MissingMarkPolicy
import com.example.data.model.TemplateAssessment
import com.example.data.model.TemplateSubject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Strict schema-v1 template codec. Only academic structure is exported, never personal records. */
object AcademicTemplateCodec {
    const val FORMAT = "clg1template"
    const val SCHEMA_VERSION = 1
    const val MAX_TEMPLATE_BYTES = 256 * 1024
    private val rootKeys = setOf(
        "format", "schema_version", "college", "program", "department", "regulation", "academic_year",
        "term", "passing_percent", "decimal_places", "combination_method", "missing_mark_policy", "best_n",
        "required_weight_percent", "grade_bands", "subjects", "verification_status", "created_at", "notes", "checksum_sha256"
    )
    private val subjectKeys = setOf("code", "name", "credits", "subject_pass_percent", "assessments")
    private val assessmentKeys = setOf("name", "maximum", "weight_percent", "category", "minimum_pass_percent")

    fun export(
        college: String,
        program: String,
        department: String,
        regulation: String,
        academicYear: String,
        term: String,
        rules: AcademicRules,
        subjects: List<AcademicSubject>,
        includeSubjectStructure: Boolean = true
    ): String {
        require(college.length <= 160 && program.length <= 120 && department.length <= 120 && regulation.length <= 80 && academicYear.length <= 40 && term.length <= 100) {
            "Template metadata is too long"
        }
        val invalidRules = com.example.domain.AcademicRulesEngine.validate(rules)
        require(invalidRules.isEmpty()) { invalidRules.joinToString("; ") }
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schema_version", SCHEMA_VERSION)
            .put("college", college.trim())
            .put("program", program.trim())
            .put("department", department.trim())
            .put("regulation", regulation.trim())
            .put("academic_year", academicYear.trim())
            .put("term", term.trim())
            .put("passing_percent", rules.passingPercent)
            .put("decimal_places", rules.decimalPlaces)
            .put("combination_method", rules.combinationMethod.name)
            .put("missing_mark_policy", rules.missingMarkPolicy.name)
            .put("best_n", rules.bestN)
            .put("required_weight_percent", rules.requiredWeightPercent ?: JSONObject.NULL)
            .put("verification_status", "COMMUNITY_TEMPLATE_NOT_VERIFIED")
            .put("created_at", System.currentTimeMillis())
            .put("notes", "Academic structure only. Verify these rules with your institution.")
        val bands = JSONArray()
        rules.gradeBands.forEach { bands.put(JSONObject().put("minimum_percent", it.minimumPercent).put("label", it.label).put("grade_point", it.gradePoint).put("inclusive", it.inclusive)) }
        root.put("grade_bands", bands)
        val subjectArray = JSONArray()
        if (includeSubjectStructure) subjects.forEach { subject ->
            require(subject.assessments.isNotEmpty()) { "Subject ${subject.name} has no assessment structure to export" }
            val errors = com.example.domain.AcademicRulesEngine.calculate(subject, rules).errors
            require(errors.isEmpty()) { "Cannot export invalid subject structure: ${errors.joinToString("; ")}" }
            val entry = JSONObject().put("code", subject.code.take(40)).put("name", subject.name.take(120)).put("credits", subject.credits)
                .put("subject_pass_percent", subject.subjectPassPercent ?: JSONObject.NULL)
            val assessmentArray = JSONArray()
            subject.assessments.forEach { a ->
                assessmentArray.put(JSONObject()
                    .put("name", a.name.take(100))
                    .put("maximum", a.maximum)
                    .put("weight_percent", a.weightPercent)
                    .put("category", a.category.take(40))
                    .put("minimum_pass_percent", a.minimumPassPercent ?: JSONObject.NULL))
            }
            entry.put("assessments", assessmentArray)
            subjectArray.put(entry)
        }
        root.put("subjects", subjectArray)
        root.put("checksum_sha256", sha256(canonical(root)))
        val output = root.toString(2)
        require(output.toByteArray(StandardCharsets.UTF_8).size <= MAX_TEMPLATE_BYTES) { "Template is too large; share a smaller structure" }
        return output
    }

    @Throws(IllegalArgumentException::class)
    fun parse(json: String): CollegeTemplate {
        require(json.toByteArray(StandardCharsets.UTF_8).size in 1..MAX_TEMPLATE_BYTES) { "Template is empty or exceeds the 256 KB size limit" }
        try {
            val root = JSONObject(json)
            requireKeys(root, rootKeys, "template")
            require(root.optString("format") == FORMAT) { "This is not a Clg 1% template" }
            require(root.optInt("schema_version", -1) == SCHEMA_VERSION) { "Unsupported template version" }
            val expected = root.optString("checksum_sha256")
            if (expected.isNotBlank()) {
                require(expected.matches(Regex("[a-fA-F0-9]{64}"))) { "Template checksum is missing or malformed" }
                val copy = JSONObject(root.toString()).apply { remove("checksum_sha256") }
                val actual = sha256(canonical(copy))
                require(actual.equals(expected, ignoreCase = true)) { "Template checksum does not match. The file may be damaged or modified." }
            }
            require(root.optString("verification_status", "COMMUNITY_TEMPLATE_NOT_VERIFIED") == "COMMUNITY_TEMPLATE_NOT_VERIFIED") { "Unsupported verification status" }

            val college = bounded(root, "college", 160)
            val program = bounded(root, "program", 120)
            val department = bounded(root, "department", 120)
            val regulation = bounded(root, "regulation", 80)
            val academicYear = bounded(root, "academic_year", 40)
            val term = bounded(root, "term", 100)
            val passing = root.optDouble("passing_percent", Double.NaN)
            val decimals = root.optInt("decimal_places", -1)
            val combination = runCatching { CombinationMethod.valueOf(root.getString("combination_method")) }.getOrNull()
                ?: throw IllegalArgumentException("Unknown combination method")
            val missing = runCatching { MissingMarkPolicy.valueOf(root.getString("missing_mark_policy")) }.getOrNull()
                ?: throw IllegalArgumentException("Unknown missing-mark policy")
            val bestN = root.optInt("best_n", 1)
            val requiredWeight = if (root.isNull("required_weight_percent")) null else root.optDouble("required_weight_percent", Double.NaN)
            val bandsJson = root.optJSONArray("grade_bands") ?: throw IllegalArgumentException("Grade bands are missing")
            require(bandsJson.length() <= 30) { "Too many grade bands" }
            val bands = (0 until bandsJson.length()).map { i ->
                val band = bandsJson.getJSONObject(i)
                requireKeys(band, setOf("minimum_percent", "label", "grade_point", "inclusive"), "grade band")
                GradeBand(band.optDouble("minimum_percent", Double.NaN), band.optString("label"), band.optDouble("grade_point", Double.NaN), band.optBoolean("inclusive", true))
            }
            val rules = AcademicRules(college, term, bands, passing, decimals, combination, missing, bestN, requiredWeight)
            val ruleErrors = com.example.domain.AcademicRulesEngine.validate(rules)
            require(ruleErrors.isEmpty()) { ruleErrors.joinToString("; ") }

            val subjectsJson = root.optJSONArray("subjects") ?: JSONArray()
            require(subjectsJson.length() <= 300) { "Too many subjects in template" }
            val subjects = (0 until subjectsJson.length()).map { i ->
                val subject = subjectsJson.getJSONObject(i)
                requireKeys(subject, subjectKeys, "subject")
                val code = bounded(subject, "code", 40)
                val name = bounded(subject, "name", 120)
                val credits = subject.optDouble("credits", Double.NaN)
                require(credits.isFinite() && credits in 0.0..100.0) { "Invalid credits in template subject ${i + 1}" }
                val assessmentsJson = subject.optJSONArray("assessments") ?: JSONArray()
                require(assessmentsJson.length() in 1..50) { "Each template subject needs 1–50 assessment components" }
                val assessments = (0 until assessmentsJson.length()).map { j ->
                    val assessment = assessmentsJson.getJSONObject(j)
                    requireKeys(assessment, assessmentKeys, "assessment")
                    val aName = bounded(assessment, "name", 100)
                    val maximum = assessment.optDouble("maximum", Double.NaN)
                    val weight = assessment.optDouble("weight_percent", Double.NaN)
                    val minPass = if (assessment.isNull("minimum_pass_percent")) null else assessment.optDouble("minimum_pass_percent", Double.NaN)
                    require(maximum.isFinite() && maximum > 0.0) { "Invalid maximum marks in $name" }
                    require(weight.isFinite() && weight in 0.0..100.0) { "Invalid component weight in $name" }
                    require(minPass == null || (minPass.isFinite() && minPass in 0.0..100.0)) { "Invalid component pass percentage in $name" }
                    TemplateAssessment(aName, maximum, weight, bounded(assessment, "category", 40), minPass)
                }
                require(assessments.map { it.name.lowercase(Locale.ROOT) }.distinct().size == assessments.size) { "Duplicate component names in $name" }
                require(assessments.sumOf { it.weightPercent } <= 100.0000001) { "Weights exceed 100% in $name" }
                val subjectPass = if (subject.isNull("subject_pass_percent")) null else subject.optDouble("subject_pass_percent", Double.NaN)
                require(subjectPass == null || (subjectPass.isFinite() && subjectPass in 0.0..100.0)) { "Invalid subject pass threshold in template subject $name" }
                TemplateSubject(code, name, credits, assessments, subjectPass)
            }
            subjects.forEach { subject ->
                val errors = com.example.domain.AcademicRulesEngine.calculate(
                    AcademicSubject(subject.code, subject.code, subject.name, subject.credits,
                        subject.assessments.map { AssessmentMark(it.name, null, it.maximum, it.weightPercent, MarkStatus.NOT_CONDUCTED, it.minimumPassPercent, it.category) },
                        subject.subjectPassPercent), rules
                ).errors
                require(errors.isEmpty()) { "Subject ${subject.name}: ${errors.joinToString("; ")}" }
            }
            val subjectKeysForDuplicateCheck = subjects.map { "${it.code.trim().lowercase(Locale.ROOT)}|${it.name.trim().lowercase(Locale.ROOT)}" }
            require(subjectKeysForDuplicateCheck.distinct().size == subjectKeysForDuplicateCheck.size) { "Template contains duplicate subjects" }
            val nonBlankCodes = subjects.map { it.code.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            require(nonBlankCodes.distinct().size == nonBlankCodes.size) { "Template contains duplicate subject codes" }
            return CollegeTemplate(college, program, department, regulation, academicYear, term, rules, subjects, expected)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Malformed template file", e)
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Template contains fields of the wrong type", e)
        }
    }

    private fun bounded(obj: JSONObject, key: String, max: Int): String {
        val value = obj.optString(key, "")
        require(value.length <= max) { "$key is too long" }
        return value.trim()
    }

    private fun requireKeys(obj: JSONObject, allowed: Set<String>, label: String) {
        val unknown = obj.keys().asSequence().filterNot { it in allowed }.toList()
        require(unknown.isEmpty()) { "Unsupported fields in $label: ${unknown.joinToString()}" }
    }

    private fun canonical(value: Any): String = when (value) {
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(prefix = "{", postfix = "}") { key ->
            JSONObject.quote(key) + ":" + canonical(value.get(key))
        }
        is JSONArray -> (0 until value.length()).joinToString(prefix = "[", postfix = "]") { canonical(value.get(it)) }
        JSONObject.NULL -> "null"
        is String -> JSONObject.quote(value)
        is Number -> when (value) {
            is Double, is Float -> {
                val d = value.toDouble()
                if (d.isFinite() && d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            is Long, is Int, is Short, is Byte -> value.toLong().toString()
            else -> value.toString()
        }
        is Boolean -> value.toString()
        else -> JSONObject.quote(value.toString())
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
