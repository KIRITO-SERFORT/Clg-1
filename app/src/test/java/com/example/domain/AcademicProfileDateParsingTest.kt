package com.example.domain

import com.example.data.template.AcademicProfileCodec
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicProfileDateParsingTest {
    @Test fun acceptsOnlyCompleteRealCalendarDates() {
        assertNotNull(AcademicProfileCodec.parseDate("2026-11-08"))
        assertNull(AcademicProfileCodec.parseDate("2026-02-30"))
        assertNull(AcademicProfileCodec.parseDate("2026-11-08garbage"))
        assertNull(AcademicProfileCodec.parseDate("08-11-2026"))
    }
}
