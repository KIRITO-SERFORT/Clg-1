package com.example.domain

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class AcademicAnalyticsTest {
    @Test fun targetCalculatorDetectsFeasibleAndImpossibleTargets() {
        val feasible = AcademicAnalytics.targetOnRemainingWeight(30.0, 50.0, 70.0)
        assertTrue(feasible.feasible)
        assertEquals(80.0, feasible.requiredPercent!!, 0.001)
        val impossible = AcademicAnalytics.targetOnRemainingWeight(10.0, 20.0, 80.0)
        assertFalse(impossible.feasible)
        assertNull(impossible.requiredPercent)
    }

    @Test fun whatIfDoesNotMutateActualMarks() {
        val subject = AcademicSubject("s1", "CS1", "Computer Science", 3.0, listOf(
            AssessmentMark("Internal", 20.0, 25.0, 40.0),
            AssessmentMark("Final", null, 100.0, 60.0, MarkStatus.NOT_CONDUCTED)
        ))
        val rules = AcademicRules("College", "Semester 1", listOf(GradeBand(40.0, "Pass", 5.0)))
        val scenario = AcademicAnalytics.whatIf(subject, rules, 1, 80.0)
        assertNotNull(scenario)
        assertNull(subject.assessments[1].obtained)
        assertEquals(80.0, scenario!!.projectedPercent, 0.001)
    }

    @Test fun bestNRejectsMoreComponentsThanConfigured() {
        val subject = AcademicSubject("s1", "A", "A", 3.0, listOf(AssessmentMark("Test", 8.0, 10.0, 100.0)))
        val rules = AcademicRules("", "", emptyList(), combinationMethod = CombinationMethod.BEST_N_OF_M, bestN = 2)
        val result = AcademicRulesEngine.calculate(subject, rules)
        assertNull(result.percentage)
        assertFalse(result.errors.isEmpty())
    }

    @Test fun sgpaTargetUsesCompletedAndRemainingCredits() {
        val subjects = listOf(
            AcademicSubject("done", "CS1", "Completed", 3.0, listOf(AssessmentMark("Final", 90.0, 100.0, 100.0))),
            AcademicSubject("todo", "CS2", "Remaining", 3.0, listOf(AssessmentMark("Final", null, 100.0, 100.0, MarkStatus.NOT_CONDUCTED)))
        )
        val rules = AcademicRules("College", "Semester 1", listOf(
            GradeBand(90.0, "O", 10.0), GradeBand(80.0, "A", 9.0), GradeBand(70.0, "B", 8.0), GradeBand(0.0, "F", 0.0)
        ))
        val result = AcademicAnalytics.targetSgpa(subjects, rules, 9.0)
        assertTrue(result.feasible)
        assertEquals(8.0, result.requiredGradePoint!!, 0.001)
    }

    @Test fun invalidIndexReturnsNoScenario() {
        val subject = AcademicSubject("s1", "A", "A", 3.0, emptyList())
        val rules = AcademicRules("", "", emptyList())
        assertNull(AcademicAnalytics.whatIf(subject, rules, 0, 20.0))
    }
}
