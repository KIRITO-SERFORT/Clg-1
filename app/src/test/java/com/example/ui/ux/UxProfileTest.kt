package com.example.ui.ux

import org.junit.Assert.assertEquals
import org.junit.Test

class UxProfileTest {
    @Test
    fun fromIdMapsKnownAndUnknownIds() {
        assertEquals(UxProfile.LEDGER, UxProfile.fromId("nope"))
        assertEquals(UxProfile.FOCUS_DESK, UxProfile.fromId("focus_desk"))
        assertEquals(UxProfile.LEDGER, UxProfile.fromId(null))
    }
}
