package com.example.data.model

/** Versioned, share-safe academic configuration. Student marks and ERP secrets are never part of this model. */
data class AcademicProfile(
    val schemaVersion: Int = 1,
    val institution: String = "",
    val university: String = "",
    val program: String = "",
    val department: String = "",
    val regulation: String = "",
    val academicYear: String = "",
    val calendarType: AcademicCalendarType = AcademicCalendarType.SEMESTER,
    val periods: List<AcademicPeriod> = emptyList(),
    val defaultScheme: AssessmentScheme = AssessmentScheme(),
    val grading: GradingScheme = GradingScheme()
)

enum class AcademicCalendarType { SEMESTER, TRIMESTER, QUARTER, ANNUAL, MODULE, CUSTOM }

data class AcademicPeriod(
    val id: String,
    val name: String,
    val order: Int,
    val startsAtEpochMillis: Long? = null,
    val endsAtEpochMillis: Long? = null,
    val examAtEpochMillis: Long? = null
)

data class AssessmentScheme(
    val components: List<AssessmentComponentDefinition> = emptyList(),
    val combinationMethod: CombinationMethod = CombinationMethod.WEIGHTED_AVERAGE,
    val missingMarkPolicy: MissingMarkPolicy = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE,
    val bestN: Int = 1,
    val requireWeightsToTotal100: Boolean = true
)

data class AssessmentComponentDefinition(
    val id: String,
    val name: String,
    val category: String = "assessment",
    val maximumMarks: Double,
    val weightPercent: Double,
    val minimumPassPercent: Double? = null,
    val required: Boolean = true
)

data class GradingScheme(
    val bands: List<GradeBand> = emptyList(),
    val passingPercent: Double = 40.0,
    val decimalPlaces: Int = 2,
    val boundaryInclusive: Boolean = true,
    val relativeGradingEnabled: Boolean = false,
    val attendanceAffectsMarks: Boolean = false,
    val attendanceMarksMaximum: Double? = null
)
