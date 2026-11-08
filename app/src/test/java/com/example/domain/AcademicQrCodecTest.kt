package com.example.domain

import com.example.util.AcademicQrCodec
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicQrCodecTest {
    @Test fun compressedTemplatePayloadRoundTripsAndCreatesQrBitmap() {
        val source = "{\"format\":\"clg1template\",\"schema_version\":1,\"college\":\"Example Institute\"}"
        val payload = AcademicQrCodec.encode(source)
        assertEquals(source, AcademicQrCodec.decode(payload))
        val bitmap = AcademicQrCodec.generateBitmap(payload, 256)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test fun rejectsUnsupportedPayloadPrefix() {
        val rejected = runCatching { AcademicQrCodec.decode("NOT-A-CLG1-QR") }.isFailure
        assertTrue(rejected)
    }

    @Test fun rejectsCompressedPayloadThatExpandsBeyondSafetyLimit() {
        val expanded = "x".repeat(300 * 1024)
        val compressed = ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(expanded.toByteArray(Charsets.UTF_8)) }
            output.toByteArray()
        }
        val payload = "CLG1T1:" + Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        assertTrue(runCatching { AcademicQrCodec.decode(payload) }.isFailure)
    }
}
