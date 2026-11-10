package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClgThemesTest {
    private val M = ClgColorMath
    private fun schemes() = ClgPalette.values().flatMap { p -> listOf(false, true).map { dark -> Triple(p, dark, ClgPaletteMath.build(p.spec(), dark)) } }
    private fun check(name: String, fg: Int, bg: Int, min: Double, failures: MutableList<String>) {
        val c = M.contrast(fg, bg)
        if (c < min) failures += "$name %.2f < $min".format(c)
    }

    @Test fun contrastMathMatchesKnownValues() {
        assertEquals(21.0, M.contrast(M.WHITE, M.BLACK), 0.001)
        assertEquals(1.0, M.contrast(M.WHITE, M.WHITE), 0.001)
        // #767676 on white is the classic "just passes AA" grey: 4.54:1
        assertEquals(4.54, M.contrast(0xFF767676.toInt(), M.WHITE), 0.01)
        assertEquals(M.BLACK, M.onColorFor(0xFFFFFF00.toInt())); assertEquals(M.WHITE, M.onColorFor(0xFF000080.toInt()))
    }

    @Test fun ensureContrastReachesTheTargetOrFallsBack() {
        val fixed = M.ensureContrast(0xFF9E9E9E.toInt(), M.WHITE, 4.5, M.BLACK)
        assertTrue(M.contrast(fixed, M.WHITE) >= 4.5)
        assertEquals(0xFF123456.toInt(), M.ensureContrast(0xFF123456.toInt(), M.WHITE, 4.5, M.BLACK))   // already fine: unchanged
    }

    @Test fun everyPaletteIsReadableInLightAndDark() {
        val failures = ArrayList<String>()
        for ((p, dark, s) in schemes()) {
            val n = "${p.label}/${if (dark) "dark" else "light"} "
            check(n + "onPrimary", s.onPrimary, s.primary, 4.5, failures); check(n + "onSecondary", s.onSecondary, s.secondary, 4.5, failures)
            check(n + "onTertiary", s.onTertiary, s.tertiary, 4.5, failures); check(n + "onError", s.onError, s.error, 4.5, failures)
            check(n + "onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5, failures)
            check(n + "onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5, failures)
            check(n + "onTertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, 4.5, failures)
            check(n + "onErrorContainer", s.onErrorContainer, s.errorContainer, 4.5, failures)
            check(n + "onBackground", s.onBackground, s.background, 4.5, failures); check(n + "onSurface", s.onSurface, s.surface, 4.5, failures)
            check(n + "onSurface/containerHighest", s.onSurface, s.surfaceContainerHighest, 4.5, failures)
            check(n + "onSurfaceVariant", s.onSurfaceVariant, s.surfaceVariant, 4.5, failures)
            check(n + "primary text on surface", s.primary, s.surface, 4.5, failures); check(n + "secondary on surface", s.secondary, s.surface, 4.5, failures)
            check(n + "tertiary on surface", s.tertiary, s.surface, 4.5, failures); check(n + "error on surface", s.error, s.surface, 4.5, failures)
            check(n + "outline", s.outline, s.surface, 3.0, failures); check(n + "inverse", s.inverseOnSurface, s.inverseSurface, 4.5, failures)
        }
        assertTrue("Low-contrast pairs:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun palettesAreDistinctAndWellFormed() {
        val all = ClgPalette.values()
        assertEquals(all.size, all.map { it.id }.toSet().size); assertEquals(all.size, all.map { it.label }.toSet().size)
        val dark = all.filter { it != ClgPalette.DYNAMIC }.map { ClgPaletteMath.build(it.spec(), true).primary }
        assertEquals("every palette needs its own primary colour in dark mode", dark.size, dark.toSet().size)
        all.forEach { assertEquals(it, ClgPalette.fromId(it.id)) }
        assertEquals(ClgPalette.BRAND, ClgPalette.fromId("nonsense")); assertEquals(ClgPalette.BRAND, ClgPalette.fromId(null))
    }

    @Test fun amoledIsTrulyBlackInDarkMode() {
        val s = ClgPaletteMath.build(ClgPalette.AMOLED.spec(), true)
        assertEquals(M.BLACK, s.background); assertEquals(M.BLACK, s.surfaceContainerLowest)
        assertTrue(M.luminance(s.surface) < 0.01)
        assertTrue(M.luminance(ClgPaletteMath.build(ClgPalette.BRAND.spec(), true).background) < 0.01)
    }

    @Test fun lightThemesAreLightAndDarkThemesAreDark() {
        for (p in ClgPalette.values()) {
            assertTrue("${p.label} light", M.luminance(ClgPaletteMath.build(p.spec(), false).background) > 0.7)
            assertTrue("${p.label} dark", M.luminance(ClgPaletteMath.build(p.spec(), true).background) < 0.05)
        }
    }

    @Test fun lerpEndpointsAndOpacity() {
        assertEquals(0xFF000000.toInt(), M.lerp(M.BLACK, M.WHITE, 0f)); assertEquals(M.WHITE, M.lerp(M.BLACK, M.WHITE, 1f))
        assertEquals(0xFF808080.toInt(), M.lerp(M.BLACK, M.WHITE, 0.5f)); assertEquals(0xFF, M.lerp(0x00123456, 0x00654321, 0.3f) ushr 24)
    }
}
