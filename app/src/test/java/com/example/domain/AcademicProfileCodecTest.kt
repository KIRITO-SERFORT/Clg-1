package com.example.domain

import com.example.data.model.*
import com.example.data.template.AcademicProfileCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicProfileCodecTest {
    @Test fun profileRoundTripsPeriodsComponentsAndGradingRules() {
        val profile = AcademicProfile(
            institution = "Example Institute", university = "Example University", program = "B.E.",
            department = "Computer Science", regulation = "2025", academicYear = "2026-27",
            calendarType = AcademicCalendarType.SEMESTER,
            periods = listOf(AcademicPeriod("sem1", "Semester 1", 0, 1_800_000L, 2_000_000L)),
            defaultScheme = AssessmentScheme(listOf(
                AssessmentComponentDefinition("internal", "Internal", "internal", 40.0, 40.0),
                AssessmentComponentDefinition("external", "External", "external", 60.0, 60.0)
            )),
            grading = GradingScheme(listOf(GradeBand(90.0, "O", 10.0), GradeBand(0.0, "F", 0.0)), 40.0, 2)
        )
        val restored = AcademicProfileCodec.decode(AcademicProfileCodec.encode(profile))
        assertEquals(profile.institution, restored.institution)
        assertEquals(profile.periods, restored.periods)
        assertEquals(profile.defaultScheme.components, restored.defaultScheme.components)
        assertEquals(profile.grading.bands, restored.grading.bands)
        assertTrue(AcademicSetupValidator.validate(restored).isEmpty())
    }
}
