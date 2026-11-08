package com.example.domain

import com.example.data.analytics.HistoricalAcademicAnalytics
import com.example.data.local.AcademicSnapshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoricalAcademicAnalyticsTest {
    @Test fun computesTrendGradeDistributionAndAssessmentAverages() {
        val latest = AcademicSnapshotEntity(
            "latest", "2026-27", "Semester 2", 200L, 2, 2, 82.0, 8.5, 6.0,
            """[{"name":"Mathematics","percentage":90,"grade":"O","passed":true,"provisional":false,"assessments":[{"category":"internal","normalized_percent":80},{"category":"external","normalized_percent":100}]},{"name":"Physics","percentage":74,"grade":"A","passed":true,"provisional":false,"assessments":[{"category":"internal","normalized_percent":70}]}]"""
        )
        val previous = latest.copy(id = "previous", termName = "Semester 1", capturedAtMillis = 100L, averagePercentage = 76.0, sgpa = 8.0)
        val result = HistoricalAcademicAnalytics.summarize(listOf(previous, latest))
        assertEquals(6.0, result.averageDelta!!, 0.001)
        assertEquals(1, result.gradeDistribution["O"])
        assertEquals(90.0, result.subjectPerformance.first().second, 0.001)
        assertEquals(75.0, result.assessmentCategoryAverages["internal"]!!, 0.001)
        assertTrue(result.trendLabel.contains("Improving"))
        assertEquals(8.25, result.overallCgpa!!, 0.001)
        assertEquals(2, result.termTrends.size)
        assertEquals("Semester 1", result.termTrends.first().termName)
        assertEquals(4, result.historicalGradeDistribution.values.sum())
        assertEquals(75.0, result.historicalAssessmentCategoryAverages["internal"]!!, 0.001)
    }
}
