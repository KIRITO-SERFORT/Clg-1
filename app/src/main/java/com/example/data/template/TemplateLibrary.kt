package com.example.data.template

import android.content.Context
import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.data.model.AssessmentMark
import com.example.data.model.CollegeTemplate
import com.example.data.model.CombinationMethod
import com.example.data.model.GradeBand
import com.example.data.model.MarkStatus
import com.example.data.model.MissingMarkPolicy
import com.example.data.model.TemplateAssessment
import com.example.data.model.TemplateSubject
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages community templates and locally saved templates for students to share and access.
 * Stores templates locally in SharedPreferences/Room so they persist across sessions.
 */
object TemplateLibrary {
    private const val PREFS_NAME = "clg1_saved_templates"
    private const val KEY_SAVED_TEMPLATES = "saved_templates_json"

    data class SavedTemplate(
        val id: String,
        val title: String,
        val college: String,
        val department: String,
        val term: String,
        val templateJson: String,
        val savedAtMillis: Long,
        val isCommunity: Boolean = false
    )

    /**
     * Standard community grading templates curated for autonomous engineering & arts colleges.
     */
    fun getCommunityTemplates(): List<CollegeTemplate> {
        val list = mutableListOf<CollegeTemplate>()

        // 1. ACGCET / Anna University Autonomous (10-Point Scale)
        val acgcetRules = AcademicRules(
            college = "ACGCET Autonomous",
            termName = "Semester 5",
            gradeBands = listOf(
                GradeBand(90.0, "O", 10.0),
                GradeBand(80.0, "A+", 9.0),
                GradeBand(70.0, "A", 8.0),
                GradeBand(60.0, "B+", 7.0),
                GradeBand(50.0, "B", 6.0),
                GradeBand(40.0, "C", 5.0),
                GradeBand(0.0, "F", 0.0)
            ),
            passingPercent = 45.0,
            decimalPlaces = 2,
            combinationMethod = CombinationMethod.WEIGHTED_AVERAGE,
            missingMarkPolicy = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE,
            bestN = 1,
            requiredWeightPercent = 100.0
        )

        val acgcetAssessments = listOf(
            TemplateAssessment("Continuous Assessment 1 (CAT 1)", 50.0, 20.0, "internal", 20.0),
            TemplateAssessment("Continuous Assessment 2 (CAT 2)", 50.0, 20.0, "internal", 20.0),
            TemplateAssessment("Assignment / Tutorial", 25.0, 10.0, "internal", null),
            TemplateAssessment("End Semester Exam", 100.0, 50.0, "external", 45.0)
        )

        val acgcetSubjects = listOf(
            TemplateSubject("CS8501", "Theory of Computation", 3.0, acgcetAssessments, 45.0),
            TemplateSubject("CS8591", "Computer Networks", 3.0, acgcetAssessments, 45.0),
            TemplateSubject("CS8592", "Object Oriented Analysis & Design", 3.0, acgcetAssessments, 45.0),
            TemplateSubject("EC8691", "Microprocessors & Microcontrollers", 3.0, acgcetAssessments, 45.0),
            TemplateSubject("CS8581", "Networks Laboratory", 2.0, listOf(
                TemplateAssessment("Continuous Evaluation", 40.0, 40.0, "practical", 20.0),
                TemplateAssessment("End Sem Practical", 60.0, 60.0, "practical", 30.0)
            ), 50.0)
        )

        list.add(
            CollegeTemplate(
                college = "ACGCET Autonomous / Anna University",
                program = "B.E.",
                department = "Computer Science & Engineering",
                regulation = "R2021 Autonomous",
                academicYear = "2026-27",
                termName = "Semester 5",
                rules = acgcetRules,
                subjects = acgcetSubjects,
                checksum = ""
            )
        )

        // 2. Autonomous Relative Grading Template
        val relativeRules = AcademicRules(
            college = "Autonomous Technical Institute",
            termName = "Semester 4",
            gradeBands = listOf(
                GradeBand(85.0, "A+", 10.0),
                GradeBand(75.0, "A", 9.0),
                GradeBand(65.0, "B+", 8.0),
                GradeBand(55.0, "B", 7.0),
                GradeBand(45.0, "C", 6.0),
                GradeBand(35.0, "D", 5.0),
                GradeBand(0.0, "F", 0.0)
            ),
            passingPercent = 35.0,
            decimalPlaces = 2,
            combinationMethod = CombinationMethod.THEORY_PRACTICAL,
            missingMarkPolicy = MissingMarkPolicy.COUNT_ABSENT_AS_ZERO
        )

        val relativeAssessments = listOf(
            TemplateAssessment("Mid-Term Test 1", 30.0, 25.0, "internal", null),
            TemplateAssessment("Mid-Term Test 2", 30.0, 25.0, "internal", null),
            TemplateAssessment("Final Comprehensive Exam", 100.0, 50.0, "external", 35.0)
        )

        val relativeSubjects = listOf(
            TemplateSubject("MA401", "Probability & Linear Algebra", 4.0, relativeAssessments, 35.0),
            TemplateSubject("CS402", "Design & Analysis of Algorithms", 4.0, relativeAssessments, 35.0),
            TemplateSubject("CS403", "Database Management Systems", 3.0, relativeAssessments, 35.0),
            TemplateSubject("CS404", "Operating Systems", 3.0, relativeAssessments, 35.0)
        )

        list.add(
            CollegeTemplate(
                college = "Autonomous Engineering College",
                program = "B.Tech",
                department = "Information Technology",
                regulation = "CBCS 2024",
                academicYear = "2026-27",
                termName = "Semester 4",
                rules = relativeRules,
                subjects = relativeSubjects,
                checksum = ""
            )
        )

        // 3. VTU Choice-Based Credit System (CIE + SEE 50:50)
        val vtuRules = AcademicRules(
            college = "VTU Affiliated / Autonomous",
            termName = "Semester 6",
            gradeBands = listOf(
                GradeBand(90.0, "O", 10.0),
                GradeBand(80.0, "A+", 9.0),
                GradeBand(70.0, "A", 8.0),
                GradeBand(60.0, "B+", 7.0),
                GradeBand(50.0, "B", 6.0),
                GradeBand(40.0, "C", 5.0),
                GradeBand(0.0, "F", 0.0)
            ),
            passingPercent = 40.0,
            decimalPlaces = 2,
            combinationMethod = CombinationMethod.WEIGHTED_AVERAGE,
            missingMarkPolicy = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE
        )

        val vtuAssessments = listOf(
            TemplateAssessment("Internal Assessment 1", 50.0, 20.0, "internal", 18.0),
            TemplateAssessment("Internal Assessment 2", 50.0, 20.0, "internal", 18.0),
            TemplateAssessment("Activity / Assignment", 20.0, 10.0, "internal", null),
            TemplateAssessment("Semester End Exam (SEE)", 100.0, 50.0, "external", 35.0)
        )

        val vtuSubjects = listOf(
            TemplateSubject("21CS61", "Software Engineering & Project Management", 3.0, vtuAssessments, 40.0),
            TemplateSubject("21CS62", "Full Stack Development", 4.0, vtuAssessments, 40.0),
            TemplateSubject("21CS63", "Cloud Computing Architecture", 3.0, vtuAssessments, 40.0)
        )

        list.add(
            CollegeTemplate(
                college = "VTU CBCS Framework",
                program = "B.E.",
                department = "Computer Science",
                regulation = "2021 Scheme",
                academicYear = "2026-27",
                termName = "Semester 6",
                rules = vtuRules,
                subjects = vtuSubjects,
                checksum = ""
            )
        )

        return list
    }

    /**
     * Converts a CollegeTemplate into full AcademicSubject instances ready to use in the workspace.
     */
    fun createSubjectsFromTemplate(template: CollegeTemplate): List<AcademicSubject> {
        return template.subjects.map { ts ->
            val assessments = ts.assessments.map { a ->
                AssessmentMark(
                    name = a.name,
                    obtained = null,
                    maximum = a.maximum,
                    weightPercent = a.weightPercent,
                    status = MarkStatus.NOT_CONDUCTED,
                    minimumPassPercent = a.minimumPassPercent,
                    category = a.category
                )
            }
            AcademicSubject(
                id = java.util.UUID.randomUUID().toString(),
                code = ts.code,
                name = ts.name,
                credits = ts.credits,
                assessments = assessments,
                subjectPassPercent = ts.subjectPassPercent
            )
        }
    }

    /**
     * Retrieves saved custom templates from local storage.
     */
    fun getSavedLocalTemplates(context: Context): List<SavedTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SAVED_TEMPLATES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SavedTemplate(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    college = obj.optString("college", ""),
                    department = obj.optString("department", ""),
                    term = obj.optString("term", ""),
                    templateJson = obj.getString("template_json"),
                    savedAtMillis = obj.optLong("saved_at", System.currentTimeMillis()),
                    isCommunity = obj.optBoolean("is_community", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Saves a template to local storage so the student can reuse and share it.
     */
    fun saveTemplateLocally(
        context: Context,
        title: String,
        template: CollegeTemplate,
        templateJson: String
    ) {
        val current = getSavedLocalTemplates(context).toMutableList()
        val newEntry = SavedTemplate(
            id = java.util.UUID.randomUUID().toString(),
            title = title.ifBlank { "${template.college} - ${template.termName}" },
            college = template.college,
            department = template.department,
            term = template.termName,
            templateJson = templateJson,
            savedAtMillis = System.currentTimeMillis(),
            isCommunity = false
        )
        current.add(0, newEntry)
        persistLocalTemplates(context, current)
    }

    /**
     * Deletes a template from local storage.
     */
    fun deleteSavedTemplate(context: Context, id: String) {
        val updated = getSavedLocalTemplates(context).filterNot { it.id == id }
        persistLocalTemplates(context, updated)
    }

    private fun persistLocalTemplates(context: Context, list: List<SavedTemplate>) {
        val array = JSONArray()
        list.take(50).forEach { item ->
            val obj = JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("college", item.college)
                .put("department", item.department)
                .put("term", item.term)
                .put("template_json", item.templateJson)
                .put("saved_at", item.savedAtMillis)
                .put("is_community", item.isCommunity)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_TEMPLATES, array.toString())
            .apply()
    }
}
