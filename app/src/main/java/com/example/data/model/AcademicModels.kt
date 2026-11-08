package com.example.data.model

/** Academic records are device-local. These models never contain ERP credentials or identity. */
data class AssessmentMark(
    val name: String,
    val obtained: Double?,
    val maximum: Double,
    val weightPercent: Double,
    val status: MarkStatus = MarkStatus.RECORDED,
    /** Optional minimum percentage required in this component to pass it. */
    val minimumPassPercent: Double? = null,
    val category: String = "assessment",
    val dateEpochMillis: Long? = null
)

enum class MarkStatus { RECORDED, ABSENT, EXEMPTED, NOT_CONDUCTED }

enum class CombinationMethod {
    /** Normalize each score to a percentage, apply the configured contribution weights, then normalize by included weights. */
    WEIGHTED_AVERAGE,
    /** Average normalized percentages equally. */
    ARITHMETIC_AVERAGE,
    /** Average the highest N normalized component percentages. */
    BEST_N_OF_M,
    /** Use the highest normalized component percentage. */
    HIGHEST,
    /** Sum obtained marks / sum maximum marks. */
    SUM,
    /** Same mathematically explicit weighted combination, intended for schemes with theory/practical components. */
    THEORY_PRACTICAL
}

enum class MissingMarkPolicy {
    /** Ignore missing, exempted and not-conducted items. Results remain provisional. */
    EXCLUDE_AND_RENORMALIZE,
    /** Treat absent and not-conducted as zero; exempted items remain excluded. */
    COUNT_ABSENT_AS_ZERO,
    /** Do not calculate a result until every non-exempt component is recorded. */
    REQUIRE_ALL_RECORDED
}

data class AcademicSubject(
    val id: String,
    val code: String,
    val name: String,
    val credits: Double,
    val assessments: List<AssessmentMark>,
    val subjectPassPercent: Double? = null
)

data class GradeBand(
    val minimumPercent: Double,
    val label: String,
    val gradePoint: Double,
    /** true: score >= boundary; false: score > boundary. */
    val inclusive: Boolean = true
)

data class AcademicRules(
    val college: String,
    val termName: String,
    val gradeBands: List<GradeBand>,
    val passingPercent: Double = 40.0,
    val decimalPlaces: Int = 2,
    val combinationMethod: CombinationMethod = CombinationMethod.WEIGHTED_AVERAGE,
    val missingMarkPolicy: MissingMarkPolicy = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE,
    val bestN: Int = 1,
    val requiredWeightPercent: Double? = null,
    val theoryPracticalCombined: Boolean = false
)

data class AssessmentContribution(
    val name: String,
    val normalizedPercent: Double,
    val appliedWeightPercent: Double,
    val weightedPoints: Double,
    val included: Boolean,
    val note: String
)

data class SubjectResult(
    val percentage: Double?,
    val grade: String?,
    val gradePoint: Double?,
    val passed: Boolean?,
    val explanation: String,
    val provisional: Boolean,
    val contributions: List<AssessmentContribution> = emptyList(),
    val errors: List<String> = emptyList(),
    val internalPercent: Double? = null,
    val practicalPercent: Double? = null,
    val externalPercent: Double? = null
)

data class AcademicTermResult(val termName: String, val credits: Double, val sgpa: Double)
