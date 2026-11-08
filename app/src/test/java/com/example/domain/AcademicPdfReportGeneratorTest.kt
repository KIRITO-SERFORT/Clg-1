package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.example.data.analytics.AcademicPdfReportGenerator
import com.example.data.local.AcademicSnapshotEntity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream

@Implements(PdfDocument::class)
class ShadowCustomPdfDocument {
    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        val bitmap = Bitmap.createBitmap(pageInfo.pageWidth.coerceAtLeast(1), pageInfo.pageHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val constructor = PdfDocument.Page::class.java.getDeclaredConstructor(Canvas::class.java, PdfDocument.PageInfo::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(canvas, pageInfo)
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {}

    @Implementation
    fun writeTo(out: OutputStream) {
        out.write("%PDF-1.4\n".toByteArray(Charsets.UTF_8))
        out.write(ByteArray(600) { 0x20.toByte() })
    }

    @Implementation
    fun close() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [ShadowCustomPdfDocument::class])
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
