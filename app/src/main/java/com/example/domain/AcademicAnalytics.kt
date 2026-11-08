package com.example.domain

import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.data.model.MarkStatus
import java.math.BigDecimal
import java.math.RoundingMode

/** Interpretable what-if and target calculations. Hypothetical values never mutate source records. */
object AcademicAnalytics {
    data class TargetResult(val requiredPercent: Double?, val feasible: Boolean, val explanation: String)
    data class ScenarioResult(val projectedPercent: Double, val label: String)
    data class SgpaTargetResult(val requiredGradePoint: Double?, val feasible: Boolean, val explanation: String)

    /**
     * Calculates the percentage needed across the remaining weighted contribution.
     * completedWeightedPoints is already expressed on the final 0..100 scale.
     */
    fun targetOnRemainingWeight(
        completedWeightedPoints: Double,
        remainingWeightPercent: Double,
        targetPercent: Double
    ): TargetResult {
        if (!completedWeightedPoints.isFinite() || completedWeightedPoints !in 0.0..100.0 ||
            !remainingWeightPercent.isFinite() || remainingWeightPercent !in 0.0..100.0 ||
            !targetPercent.isFinite() || targetPercent !in 0.0..100.0) {
            return TargetResult(null, false, "Inputs must be finite percentages between 0 and 100.")
        }
        if (remainingWeightPercent == 0.0) {
            val reached = completedWeightedPoints + 1e-9 >= targetPercent
            return TargetResult(if (reached) 0.0 else null, reached,
                if (reached) "The target has already been reached." else "No remaining assessment weight is available; the target is impossible.")
        }
        val needed = (targetPercent - completedWeightedPoints) / remainingWeightPercent * 100.0
        return when {
            needed <= 0.0 -> TargetResult(0.0, true, "The target has already been reached; no additional marks are required.")
            needed > 100.0 -> TargetResult(null, false, "The target is not achievable with the remaining assessment weight.")
            else -> TargetResult(round(needed), true, "You need at least ${round(needed)}% across the remaining assessment weight.")
        }
    }

    /**
     * Computes the grade point needed across subjects that are not yet final to reach a target SGPA.
     * Completed subjects are weighted by their configured credits; provisional subjects remain in the remaining-credit pool.
     */
    fun targetSgpa(subjects: List<AcademicSubject>, rules: AcademicRules, targetSgpa: Double): SgpaTargetResult {
        if (subjects.isEmpty() || !targetSgpa.isFinite() || targetSgpa < 0.0) {
            return SgpaTargetResult(null, false, "Add subjects and enter a valid target SGPA.")
        }
        val maxGradePoint = rules.gradeBands.maxOfOrNull { it.gradePoint }
            ?: return SgpaTargetResult(null, false, "Configure grade-point bands before using the SGPA target simulator.")
        if (targetSgpa > maxGradePoint) return SgpaTargetResult(null, false, "Target SGPA exceeds the highest configured grade point ($maxGradePoint).")
        val results = subjects.map { it to AcademicRulesEngine.calculate(it, rules) }
        val invalid = results.firstOrNull { it.first.credits <= 0.0 || it.second.errors.isNotEmpty() || (!it.second.provisional && it.second.gradePoint == null) }
        if (invalid != null) return SgpaTargetResult(null, false, "Resolve invalid subject credits, unmapped grade bands or academic rules before calculating target SGPA.")
        val completed = results.filter { !it.second.provisional && it.second.gradePoint != null }
        val remaining = results.filter { it.second.provisional || it.second.gradePoint == null }
        val completedCredits = completed.sumOf { it.first.credits }
        val remainingCredits = remaining.sumOf { it.first.credits }
        val totalCredits = completedCredits + remainingCredits
        if (totalCredits <= 0.0) return SgpaTargetResult(null, false, "No valid subject credits are available.")
        val completedGradePoints = completed.sumOf { it.first.credits * (it.second.gradePoint ?: 0.0) }
        if (remainingCredits <= 0.0) {
            val actual = completedGradePoints / completedCredits
            val reached = actual + 1e-9 >= targetSgpa
            return SgpaTargetResult(if (reached) 0.0 else null, reached,
                if (reached) "The target SGPA has already been reached." else "All subjects are final; the target SGPA cannot be changed in this term.")
        }
        val needed = (targetSgpa * totalCredits - completedGradePoints) / remainingCredits
        return when {
            needed <= 0.0 -> SgpaTargetResult(0.0, true, "Completed subjects already provide enough grade points for the target SGPA.")
            needed > maxGradePoint -> SgpaTargetResult(null, false, "You would need ${round(needed)} grade points per remaining credit; the configured maximum is $maxGradePoint.")
            else -> SgpaTargetResult(round(needed), true, "You need an average of ${round(needed)} grade points across the remaining ${round(remainingCredits)} credits.")
        }
    }

    /** Returns a hypothetical result without changing subject.assessments. */
    fun whatIf(subject: AcademicSubject, rules: AcademicRules, hypotheticalAssessmentIndex: Int, hypotheticalMarks: Double): ScenarioResult? {
        if (hypotheticalAssessmentIndex !in subject.assessments.indices) return null
        val target = subject.assessments[hypotheticalAssessmentIndex]
        if (!hypotheticalMarks.isFinite() || hypotheticalMarks !in 0.0..target.maximum) return null
        val changed = subject.copy(assessments = subject.assessments.toMutableList().also { list ->
            list[hypotheticalAssessmentIndex] = target.copy(obtained = hypotheticalMarks, status = MarkStatus.RECORDED)
        })
        val result = AcademicRulesEngine.calculate(changed, rules)
        val percentage = result.percentage ?: return null
        return ScenarioResult(percentage, if (result.provisional) "Estimate · provisional" else "What-if scenario")
    }

    /** Projection scenarios are intentionally deterministic and never imply statistical confidence. */
    fun scenarios(subject: AcademicSubject, rules: AcademicRules, remainingAssessmentIndex: Int): List<ScenarioResult> {
        if (remainingAssessmentIndex !in subject.assessments.indices) return emptyList()
        val maximum = subject.assessments[remainingAssessmentIndex].maximum
        return listOf(0.5, 0.7, 0.9).mapIndexedNotNull { index, fraction ->
            whatIf(subject, rules, remainingAssessmentIndex, maximum * fraction)?.copy(
                label = listOf("Conservative estimate", "Expected estimate", "Optimistic estimate")[index]
            )
        }
    }

    private fun round(value: Double): Double = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
