package com.example.data.backup

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Passphrase-encrypted backup envelope (AES-256-GCM + PBKDF2-HMAC-SHA256). */
object EncryptedAcademicBackup {
    private const val HEADER = "CLG1-ACADEMIC-BACKUP"
    private const val VERSION = "1"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val MIN_PASSPHRASE_LENGTH = 10
    private const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
    private const val MAX_ENVELOPE_BYTES = 6 * 1024 * 1024
    private val random = SecureRandom()

    fun encrypt(plainText: String, passphrase: CharArray): ByteArray {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) { "Use a backup passphrase with at least 10 characters" }
        val plain = plainText.toByteArray(StandardCharsets.UTF_8)
        require(plain.size <= MAX_BACKUP_BYTES) { "Backup exceeds the 4 MB size limit" }
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(passphrase, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.updateAAD("$HEADER:$VERSION".toByteArray(StandardCharsets.UTF_8))
            val encrypted = cipher.doFinal(plain)
            val envelope = buildString {
                append(HEADER).append('\n')
                append(VERSION).append('\n')
                append(ITERATIONS).append('\n')
                append(Base64.encodeToString(salt, Base64.NO_WRAP)).append('\n')
                append(Base64.encodeToString(iv, Base64.NO_WRAP)).append('\n')
                append(Base64.encodeToString(encrypted, Base64.NO_WRAP)).append('\n')
            }
            envelope.toByteArray(StandardCharsets.UTF_8)
        } finally {
            key.encoded?.fill(0)
            plain.fill(0)
        }
    }

    fun decrypt(envelopeBytes: ByteArray, passphrase: CharArray): String {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) { "Backup passphrase must contain at least 10 characters" }
        require(envelopeBytes.size in 1..MAX_ENVELOPE_BYTES) { "Backup file is empty or too large" }
        val parts = envelopeBytes.toString(StandardCharsets.UTF_8).trim().split('\n')
        require(parts.size == 6 && parts[0] == HEADER && parts[1] == VERSION) { "Unsupported or malformed backup file" }
        val iterations = parts[2].toIntOrNull() ?: throw IllegalArgumentException("Invalid backup KDF setting")
        require(iterations in 100_000..1_000_000) { "Unsafe backup KDF setting" }
        val salt = decodeBase64(parts[3], 16)
        val iv = decodeBase64(parts[4], 12)
        val encrypted = try { Base64.decode(parts[5], Base64.NO_WRAP) } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Backup payload is malformed")
        }
        require(encrypted.size in 16..(MAX_BACKUP_BYTES + 16)) { "Backup payload size is invalid" }
        val key = deriveKey(passphrase, salt, iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.updateAAD("$HEADER:$VERSION".toByteArray(StandardCharsets.UTF_8))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("Backup could not be decrypted. Check the passphrase or file integrity.")
        } finally {
            key.encoded?.fill(0)
            salt.fill(0)
            iv.fill(0)
            encrypted.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun decodeBase64(value: String, expectedLength: Int): ByteArray {
        val decoded = try { Base64.decode(value, Base64.NO_WRAP) } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Backup cryptographic header is malformed")
        }
        require(decoded.size == expectedLength) { "Backup cryptographic header has an invalid length" }
        return decoded
    }
}
