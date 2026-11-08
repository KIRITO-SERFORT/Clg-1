package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.util.AcademicQrCodec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the QR encoder on Android's real graphics runtime (scanner camera test is manual). */
@RunWith(AndroidJUnit4::class)
class AcademicQrDeviceTest {
    @Test fun qrTemplateBitmapIsCreatedAtExpectedDimensions() {
        val template = """{"format":"clg1template","schema_version":1,"subjects":[]}"""
        val payload = AcademicQrCodec.encode(template)
        assertEquals(template, AcademicQrCodec.decode(payload))
        val bitmap = AcademicQrCodec.generateBitmap(payload, 512)
        try {
            assertEquals(512, bitmap.width)
            assertEquals(512, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }
}
