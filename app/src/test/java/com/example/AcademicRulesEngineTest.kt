package com.example

import com.example.data.model.*
import com.example.domain.AcademicRulesEngine
import org.junit.Assert.*
import org.junit.Test

class AcademicRulesEngineTest {
    private val bands = listOf(
        GradeBand(90.0, "O", 10.0), GradeBand(80.0, "A+", 9.0),
        GradeBand(70.0, "A", 8.0), GradeBand(60.0, "B+", 7.0),
        GradeBand(50.0, "B", 6.0), GradeBand(40.0, "C", 5.0)
    )
    private val rules = AcademicRules("Test College", "Semester 1", bands, 40.0)

    @Test fun weightedNormalizedScoreIsCalculated() {
        val subject = AcademicSubject("1", "CS101", "Programming", 4.0, listOf(
            AssessmentMark("Internal", 18.0, 20.0, 40.0),
            AssessmentMark("Assignment", 45.0, 50.0, 60.0)
        ))
        val result = AcademicRulesEngine.calculate(subject, rules)
        assertEquals(90.0, result.percentage!!, 0.001)
        assertEquals("O", result.grade)
        assertFalse(result.provisional)
        assertTrue(result.explanation.contains("weighted normalized average"))
    }

    @Test fun missingAssessmentIsNotTreatedAsZero() {
        val subject = AcademicSubject("1", "CS101", "Programming", 4.0, listOf(
            AssessmentMark("Internal", 16.0, 20.0, 50.0),
            AssessmentMark("Final", null, 80.0, 50.0, MarkStatus.NOT_CONDUCTED)
        ))
        val result = AcademicRulesEngine.calculate(subject, rules)
        assertEquals(80.0, result.percentage!!, 0.001)
        assertTrue(result.provisional)
    }

    @Test fun invalidMarksAreRejected() {
        val subject = AcademicSubject("1", "CS101", "Programming", 3.0, listOf(AssessmentMark("Test", 30.0, 20.0, 100.0)))
        assertNull(AcademicRulesEngine.calculate(subject, rules).percentage)
    }

    @Test fun sgpaUsesCreditWeightedGradePointsAndOnlyFinalSubjects() {
        val a = AcademicSubject("1", "A", "A", 3.0, listOf(AssessmentMark("Test", 95.0, 100.0, 100.0)))
        val b = AcademicSubject("2", "B", "B", 1.0, listOf(AssessmentMark("Test", 85.0, 100.0, 100.0)))
        assertEquals(9.75, AcademicRulesEngine.sgpa(listOf(a,b), rules)!!, 0.001)
        val pending = b.copy(assessments = listOf(AssessmentMark("Internal", null, 100.0, 100.0, MarkStatus.NOT_CONDUCTED)))
        assertNull(AcademicRulesEngine.sgpa(listOf(a, pending), rules))
    }

    @Test fun bestNUsesHighestNormalizedScores() {
        val subject = AcademicSubject("1", "M1", "Maths", 4.0, listOf(
            AssessmentMark("Test 1", 50.0, 100.0, 30.0),
            AssessmentMark("Test 2", 90.0, 100.0, 30.0),
            AssessmentMark("Test 3", 80.0, 100.0, 40.0)
        ))
        val result = AcademicRulesEngine.calculate(subject, rules.copy(combinationMethod = CombinationMethod.BEST_N_OF_M, bestN = 2))
        assertEquals(85.0, result.percentage!!, 0.001)
    }

    @Test fun arithmeticAverageAndSumRespectMaximumMarks() {
        val subject = AcademicSubject("1", "M2", "Physics", 4.0, listOf(
            AssessmentMark("Internal", 18.0, 20.0, 50.0),
            AssessmentMark("Practical", 40.0, 50.0, 50.0)
        ))
        assertEquals(85.0, AcademicRulesEngine.calculate(subject, rules.copy(combinationMethod = CombinationMethod.ARITHMETIC_AVERAGE)).percentage!!, 0.001)
        assertEquals(82.8571, AcademicRulesEngine.calculate(subject, rules.copy(combinationMethod = CombinationMethod.SUM, decimalPlaces = 4)).percentage!!, 0.001)
    }

    @Test fun absentCanBeConfiguredAsZeroOrRequireCompletion() {
        val subject = AcademicSubject("1", "M3", "Chemistry", 3.0, listOf(
            AssessmentMark("Internal", 80.0, 100.0, 50.0),
            AssessmentMark("Test", null, 100.0, 50.0, MarkStatus.ABSENT)
        ))
        val zero = AcademicRulesEngine.calculate(subject, rules.copy(missingMarkPolicy = MissingMarkPolicy.COUNT_ABSENT_AS_ZERO))
        assertEquals(40.0, zero.percentage!!, 0.001)
        assertFalse(zero.passed!!)
        val requireAll = AcademicRulesEngine.calculate(subject, rules.copy(missingMarkPolicy = MissingMarkPolicy.REQUIRE_ALL_RECORDED))
        assertNull(requireAll.percentage)
    }

    @Test fun componentPassRuleCanFailOtherwisePassingSubject() {
        val subject = AcademicSubject("1", "M4", "Drawing", 2.0, listOf(
            AssessmentMark("Theory", 90.0, 100.0, 50.0),
            AssessmentMark("Practical", 35.0, 100.0, 50.0, minimumPassPercent = 40.0)
        ))
        val result = AcademicRulesEngine.calculate(subject, rules)
        assertEquals(62.5, result.percentage!!, 0.001)
        assertFalse(result.passed!!)
    }

    @Test fun exclusiveGradeBoundaryUsesStrictComparison() {
        val exclusive = rules.copy(gradeBands = listOf(GradeBand(80.0, "A", 8.0, inclusive = false), GradeBand(0.0, "B", 5.0)))
        val exactly80 = AcademicSubject("1", "X", "X", 1.0, listOf(AssessmentMark("Test", 80.0, 100.0, 100.0)))
        assertEquals("B", AcademicRulesEngine.calculate(exactly80, exclusive).grade)
    }

    @Test fun creditWeightedCgpa() {
        assertEquals(8.5, AcademicRulesEngine.cgpa(listOf(AcademicTermResult("T1", 20.0, 8.0), AcademicTermResult("T2", 10.0, 9.5)))!!, 0.001)
        assertNull(AcademicRulesEngine.cgpa(listOf(AcademicTermResult("T1", 0.0, 8.0))))
    }

    @Test fun contributionBreakdownUsesTheSelectedCombinationWeights() {
        val subject = AcademicSubject("1", "M5", "History", 3.0, listOf(
            AssessmentMark("Quiz", 50.0, 100.0, 20.0),
            AssessmentMark("Test", 90.0, 100.0, 30.0),
            AssessmentMark("Final", 80.0, 100.0, 50.0)
        ))
        val result = AcademicRulesEngine.calculate(subject, rules.copy(combinationMethod = CombinationMethod.BEST_N_OF_M, bestN = 2))
        assertEquals(85.0, result.percentage!!, 0.001)
        assertFalse(result.contributions[0].included)
        assertEquals(100.0, result.contributions.filter { it.included }.sumOf { it.appliedWeightPercent }, 0.001)
        assertEquals(85.0, result.contributions.sumOf { it.weightedPoints }, 0.001)
    }

    @Test fun exposesInternalPracticalAndExternalSubtotals() {
        val subject = AcademicSubject("1", "M6", "Engineering", 4.0, listOf(
            AssessmentMark("Internal", 80.0, 100.0, 30.0, category = "internal"),
            AssessmentMark("Lab", 70.0, 100.0, 20.0, category = "practical"),
            AssessmentMark("End semester", 90.0, 100.0, 50.0, category = "external")
        ))
        val result = AcademicRulesEngine.calculate(subject, rules)
        assertEquals(80.0, result.internalPercent!!, 0.001)
        assertEquals(70.0, result.practicalPercent!!, 0.001)
        assertEquals(90.0, result.externalPercent!!, 0.001)
    }

    @Test fun impossibleTargetReturnsNull() = assertNull(AcademicRulesEngine.requiredMarks(70.0, 20.0, 95.0))

    @Test fun nonFiniteInputsAreRejected() {
        val subject = AcademicSubject("1", "X", "X", 1.0, listOf(AssessmentMark("Test", Double.NaN, 100.0, 100.0)))
        assertNull(AcademicRulesEngine.calculate(subject, rules).percentage)
    }
}
