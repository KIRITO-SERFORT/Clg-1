package com.example.domain

import com.example.data.model.*
import com.example.data.template.AcademicTemplateCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicTemplateCodecTest {
    @Test fun structureOnlyTemplateRoundTripsWithoutMarksOrPersonalFields() {
        val rules = AcademicRules("Example Institute", "Semester 1", listOf(
            GradeBand(90.0, "O", 10.0), GradeBand(0.0, "F", 0.0)
        ))
        val subject = AcademicSubject("s1", "CS101", "Programming", 4.0, listOf(
            AssessmentMark("Internal", 18.0, 20.0, 40.0, category = "internal"),
            AssessmentMark("Final", 72.0, 80.0, 60.0, category = "external")
        ), subjectPassPercent = 50.0)
        val encoded = AcademicTemplateCodec.export("Example Institute", "B.E.", "CSE", "R2025", "2026-27", "Semester 1", rules, listOf(subject))
        assertTrue(!encoded.contains("18.0"))
        assertTrue(!encoded.contains("72.0"))
        val restored = AcademicTemplateCodec.parse(encoded)
        assertEquals(50.0, restored.subjects.single().subjectPassPercent!!, 0.001)
        assertTrue(restored.subjects.single().assessments.all { true })
    }
}
