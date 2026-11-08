package com.example.domain

import com.example.data.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicSetupValidatorTest {
    @Test fun acceptsAValidMultiTermProfile() {
        val profile = AcademicProfile(
            institution = "Example Institute",
            academicYear = "2026-27",
            calendarType = AcademicCalendarType.TRIMESTER,
            periods = listOf(AcademicPeriod("t1", "Trimester 1", 1), AcademicPeriod("t2", "Trimester 2", 2)),
            defaultScheme = AssessmentScheme(
                components = listOf(
                    AssessmentComponentDefinition("i1", "Internal 1", maximumMarks = 25.0, weightPercent = 40.0),
                    AssessmentComponentDefinition("final", "Final", maximumMarks = 100.0, weightPercent = 60.0)
                )
            ),
            grading = GradingScheme(bands = listOf(GradeBand(90.0, "O", 10.0), GradeBand(80.0, "A+", 9.0)))
        )
        assertTrue(AcademicSetupValidator.validate(profile).isEmpty())
    }

    @Test fun rejectsWeightsThatDoNotTotalOneHundred() {
        val scheme = AssessmentScheme(components = listOf(
            AssessmentComponentDefinition("a", "A", maximumMarks = 20.0, weightPercent = 30.0),
            AssessmentComponentDefinition("b", "B", maximumMarks = 80.0, weightPercent = 50.0)
        ))
        assertTrue(AcademicSetupValidator.validate(scheme).any { it.contains("total 100%") })
    }

    @Test fun flagsInvalidPeriodDates() {
        val profile = AcademicProfile(periods = listOf(AcademicPeriod("s1", "Semester 1", 1, 200L, 100L)))
        assertFalse(AcademicSetupValidator.validate(profile).isEmpty())
    }
}
