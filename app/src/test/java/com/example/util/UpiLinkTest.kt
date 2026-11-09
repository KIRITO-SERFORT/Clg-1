package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiLinkTest {
    @Test fun validatesUpiIds() {
        assertTrue(UpiLink.isValidVpa("arunsanna071@okaxis"))
        assertTrue(UpiLink.isValidVpa("name.surname-1@ybl"))
        assertFalse(UpiLink.isValidVpa("no-at-sign"))
        assertFalse(UpiLink.isValidVpa("a@b"))                 // handle too short
        assertFalse(UpiLink.isValidVpa("bad id@okaxis"))
        assertFalse(UpiLink.isValidVpa("x@@okaxis"))
        assertFalse(UpiLink.isValidVpa(""))
    }

    @Test fun buildsPersonToPersonLink() {
        assertEquals(
            "upi://pay?pa=arunsanna071@okaxis&pn=Arun%20Sanna&am=20.00&cu=INR&tn=Tea%20for%20Clg%201",
            UpiLink.build("arunsanna071@okaxis", "Arun Sanna", 20, "Tea for Clg 1")
        )
    }

    @Test fun noAmountMeansNoAmParameter() {
        val link = UpiLink.build("arunsanna071@okaxis", "Arun Sanna", null, "Tea")
        assertFalse(link.contains("am="))
        assertTrue(link.contains("cu=INR"))
        assertFalse(link.contains("mc="))                      // never a merchant link
    }

    @Test fun rejectsOutOfRangeAmounts() {
        assertNull(UpiLink.amount(0)); assertNull(UpiLink.amount(-5)); assertNull(UpiLink.amount(100_001))
        assertEquals("1.00", UpiLink.amount(1)); assertEquals("100000.00", UpiLink.amount(100_000))
    }

    @Test fun encodesSpecialCharactersAndUnicode() {
        assertEquals("a%20b%26c%3Dd%25", UpiLink.encode("a b&c=d%"))
        assertEquals("%E0%AE%85", UpiLink.encode("\u0B85"))
        assertEquals("Az09-._~", UpiLink.encode("Az09-._~"))
    }

    @Test fun cleansNotes() {
        assertEquals("Tea for Clg 1", UpiLink.cleanNote("  Tea   for Clg 1%  "))   // % removed
        assertEquals("", UpiLink.cleanNote("\u2615\u2615"))
        assertEquals(40, UpiLink.cleanNote("x".repeat(100)).length)
    }

    @Test(expected = IllegalArgumentException::class) fun invalidPayeeThrows() { UpiLink.build("nonsense", "N", 10, "t") }

    @Test fun parsesAppResponses() {
        assertEquals(UpiLink.Outcome.SUCCESS, UpiLink.parseResult("txnId=AXI123&responseCode=00&Status=SUCCESS&txnRef=9", null).outcome)
        assertEquals("AXI123", UpiLink.parseResult("txnId=AXI123&Status=SUCCESS", null).txnId)
        assertEquals(UpiLink.Outcome.FAILURE, UpiLink.parseResult("Status=FAILURE", null).outcome)
        assertEquals(UpiLink.Outcome.PENDING, UpiLink.parseResult("status=Submitted", null).outcome)
        assertEquals(UpiLink.Outcome.SUCCESS, UpiLink.parseResult(null, "Success").outcome)
        assertEquals(UpiLink.Outcome.UNKNOWN, UpiLink.parseResult(null, null).outcome)
        assertEquals(UpiLink.Outcome.UNKNOWN, UpiLink.parseResult("garbage&&=", "weird").outcome)
        assertTrue(UpiLink.message(UpiLink.Result(UpiLink.Outcome.UNKNOWN, null)).contains("check your bank"))
    }
}
