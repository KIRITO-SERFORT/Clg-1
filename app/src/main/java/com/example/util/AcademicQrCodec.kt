package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Compact, versioned QR transport for small structure-only templates. */
object AcademicQrCodec {
    private const val PREFIX = "CLG1T1:"
    private const val MAX_QR_TEXT = 2800
    private const val MAX_DECOMPRESSED_BYTES = 256 * 1024

    fun encode(templateJson: String): String {
        val compressed = ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(templateJson.toByteArray(Charsets.UTF_8)) }
            output.toByteArray()
        }
        val payload = PREFIX + Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        require(payload.length <= MAX_QR_TEXT) { "This template is too large for a QR code. Share the .clg1template file instead." }
        return payload
    }

    fun decode(payload: String): String {
        require(payload.length in PREFIX.length + 1..MAX_QR_TEXT) { "QR template payload has an invalid size" }
        require(payload.startsWith(PREFIX)) { "This QR code is not a supported Clg 1% template" }
        val encoded = payload.removePrefix(PREFIX)
        val bytes = try { Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) } catch (_: IllegalArgumentException) { throw IllegalArgumentException("QR template encoding is invalid") }
        val expanded = GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var total = 0
            while (true) {
                val read = gzip.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_DECOMPRESSED_BYTES) { "QR template is too large after decompression" }
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
        return expanded.toString(Charsets.UTF_8)
    }

    fun generateBitmap(payload: String, size: Int = 900): Bitmap {
        require(size in 128..2048) { "QR image size must be between 128 and 2048 pixels" }
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"))
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
    }
}
