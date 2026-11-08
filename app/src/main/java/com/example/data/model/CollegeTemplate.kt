package com.example.data.model

/** Structure-only shareable configuration. Deliberately excludes marks, attendance and credentials. */
data class TemplateAssessment(
    val name: String,
    val maximum: Double,
    val weightPercent: Double,
    val category: String,
    val minimumPassPercent: Double?
)

data class TemplateSubject(
    val code: String,
    val name: String,
    val credits: Double,
    val assessments: List<TemplateAssessment>,
    val subjectPassPercent: Double? = null
)

data class CollegeTemplate(
    val college: String,
    val program: String,
    val department: String,
    val regulation: String,
    val academicYear: String,
    val termName: String,
    val rules: AcademicRules,
    val subjects: List<TemplateSubject>,
    val checksum: String
)
