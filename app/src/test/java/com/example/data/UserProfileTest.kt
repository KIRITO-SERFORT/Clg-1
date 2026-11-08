package com.example.data

import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserProfileTest {

    @Test
    fun testUserProfileDataModel() {
        val profile = UserProfile(
            userId = "test_uid_123",
            name = "Alex Mercer",
            collegeName = "St. Joseph's Engineering",
            batch = "2024 - 2028",
            isPlus = true
        )

        assertEquals("test_uid_123", profile.userId)
        assertEquals("Alex Mercer", profile.name)
        assertEquals("St. Joseph's Engineering", profile.collegeName)
        assertEquals("2024 - 2028", profile.batch)
        assertEquals(true, profile.isPlus)
    }

    @Test
    fun testUserProfileDefaults() {
        val emptyProfile = UserProfile()
        assertEquals("", emptyProfile.userId)
        assertEquals("", emptyProfile.name)
        assertEquals("", emptyProfile.collegeName)
        assertEquals("", emptyProfile.batch)
        assertFalse(emptyProfile.isPlus)
    }
}
