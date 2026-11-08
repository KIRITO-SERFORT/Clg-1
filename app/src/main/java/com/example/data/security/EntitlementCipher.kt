package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Dedicated Android-Keystore AES-GCM key for the Google Play purchase token; separate from ERP secrets. */
object EntitlementCipher {
    private const val KEY_ALIAS = "clg1_play_entitlement_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "v1:"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String? {
        return try {
            if (!value.startsWith(PREFIX)) null else {
                val parts = value.removePrefix(PREFIX).split(":")
                if (parts.size != 2) null else {
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
                    if (iv.size != 12 || encrypted.size < 16) null else {
                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
                        String(cipher.doFinal(encrypted), Charsets.UTF_8)
                    }
                }
            }
        } catch (_: Exception) { null }
    }
}
