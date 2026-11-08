package com.example.domain

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.analytics.AcademicPdfReportGenerator
import com.example.data.local.AcademicSnapshotEntity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicPdfReportGeneratorTest {
    @Test fun generatesShareablePdfWithHistoricalSnapshot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = AcademicSnapshotEntity(
            id = "report-test", academicYear = "2026-27", termName = "Semester 1",
            capturedAtMillis = 1L, subjectCount = 1, completedSubjectCount = 1,
            averagePercentage = 82.5, sgpa = 8.5, credits = 4.0,
            subjectSummariesJson = """[{"code":"CS101","name":"Programming","percentage":82.5,"grade":"A+","provisional":false,"assessments":[{"category":"internal","normalized_percent":82.5}]}]"""
        )
        val file = AcademicPdfReportGenerator.generate(context, listOf(snapshot))
        try {
            assertTrue(file.exists())
            assertTrue(file.length() > 500L)
            val header = file.inputStream().use { input -> ByteArray(5).also { input.read(it) }.decodeToString() }
            assertTrue(header.startsWith("%PDF-"))
        } finally {
            file.delete()
        }
    }

    @Test fun refusesToGenerateReportWithoutArchivedHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val failure = runCatching { AcademicPdfReportGenerator.generate(context, emptyList()) }
        assertTrue(failure.isFailure)
    }
}
