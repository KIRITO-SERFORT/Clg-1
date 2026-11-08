package com.example

import com.example.data.backup.EncryptedAcademicBackup
import org.junit.Assert.*
import org.junit.Test

class EncryptedAcademicBackupTest {
    @Test fun backupRoundTripsAndIsAuthenticated() {
        val plain = "{\"schema_version\":2,\"marks\":[12,18]}"
        val encrypted = EncryptedAcademicBackup.encrypt(plain, "very-long-passphrase".toCharArray())
        assertFalse(String(encrypted).contains("12,18"))
        assertEquals(plain, EncryptedAcademicBackup.decrypt(encrypted, "very-long-passphrase".toCharArray()))
        try {
            EncryptedAcademicBackup.decrypt(encrypted, "incorrect-passphrase".toCharArray())
            fail("Wrong password should not decrypt")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun shortPassphraseRejected() {
        try {
            EncryptedAcademicBackup.encrypt("{}", "short".toCharArray())
            fail("Short passphrase should be rejected")
        } catch (_: IllegalArgumentException) { }
    }
}
