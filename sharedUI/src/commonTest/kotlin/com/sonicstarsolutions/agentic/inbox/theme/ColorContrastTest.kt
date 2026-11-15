package com.sonicstarsolutions.agentic.inbox.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Enforces the contrast floors in DESIGN_SYSTEM.md ("Pairing and muting") against the real
 * schemes in `Theme.kt`: text pairs reach 4.5:1, icon-only state colors reach 3:1.
 */
class ColorContrastTest {

    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private val schemes: List<Triple<String, ColorScheme, AirmailColors>> = listOf(
        Triple("light", LightColorScheme, LightAirmailColors),
        Triple("dark", DarkColorScheme, DarkAirmailColors),
    )

    private fun textPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary on primary", scheme.onPrimary, scheme.primary),
        Triple("onPrimaryContainer on primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
        Triple("onSecondary on secondary", scheme.onSecondary, scheme.secondary),
        Triple("onSecondaryContainer on secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
        Triple("onTertiary on tertiary", scheme.onTertiary, scheme.tertiary),
        Triple("onTertiaryContainer on tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
        Triple("onError on error", scheme.onError, scheme.error),
        Triple("onErrorContainer on errorContainer", scheme.onErrorContainer, scheme.errorContainer),
        Triple("onSurface on surface", scheme.onSurface, scheme.surface),
        Triple("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
        Triple("onBackground on background", scheme.onBackground, scheme.background),
        Triple("onSurface on surfaceContainerLow", scheme.onSurface, scheme.surfaceContainerLow),
        Triple("onSurface on surfaceContainer", scheme.onSurface, scheme.surfaceContainer),
        Triple("onSurfaceVariant on surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer),
        Triple("inverseOnSurface on inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface),
        // Unread time, app bar title, day header names, links: navy text on the page.
        Triple("primary on surface", scheme.primary, scheme.surface),
    )

    @Test
    fun `every text pair clears 4_5 to 1 in both schemes`() {
        for ((schemeName, scheme, _) in schemes) {
            for ((label, foreground, background) in textPairs(scheme)) {
                val ratio = contrast(foreground, background)
                assertTrue(ratio >= 4.5, "$schemeName: $label has contrast $ratio")
            }
        }
    }

    @Test
    fun `the star clears 3 to 1 on the surface in both schemes`() {
        for ((schemeName, scheme, airmail) in schemes) {
            val ratio = contrast(airmail.star, scheme.surface)
            assertTrue(ratio >= 3.0, "$schemeName: star has contrast $ratio")
        }
    }

    @Test
    fun `a starred star stays visible on a selected row`() {
        // A selected row sets the star to onPrimaryContainer, so the tinted fill must still
        // separate from the surface by tone alone.
        for ((schemeName, scheme, _) in schemes) {
            val ratio = contrast(scheme.onPrimaryContainer, scheme.primaryContainer)
            assertTrue(ratio >= 3.0, "$schemeName: selected-row icon has contrast $ratio")
        }
    }

    @Test
    fun `the band colors match the design system table`() {
        assertEquals(Color(0xFF1D3557), LightAirmailColors.airmailNavy)
        assertEquals(Color(0xFFC8372D), LightAirmailColors.airmailRed)
        assertEquals(Color(0xFFAFC8F0), DarkAirmailColors.airmailNavy)
        assertEquals(Color(0xFFF0958C), DarkAirmailColors.airmailRed)
    }

    @Test
    fun `navy primary is the seed in light mode`() {
        assertEquals(Seed, LightColorScheme.primary)
    }
}
