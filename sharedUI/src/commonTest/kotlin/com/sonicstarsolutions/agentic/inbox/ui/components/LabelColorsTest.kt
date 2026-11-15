package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.ui.graphics.Color
import com.sonicstarsolutions.agentic.inbox.domain.model.LabelColors
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LabelColorsTest {

    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun `every colour the server accepts has its own tonal pair`() {
        val pairs = LabelColors.all.map { labelColorPair(it) }

        assertEquals(LabelColors.all.size, pairs.toSet().size, "each colour name must look different")
    }

    @Test
    fun `every pair clears the design system's 4_5 to 1 contrast floor`() {
        for (name in LabelColors.all) {
            val (container, content) = labelColorPair(name)
            val ratio = contrast(container, content)
            assertTrue(ratio >= 4.5, "$name has contrast $ratio")
        }
    }

    @Test
    fun `an unknown colour falls back to slate instead of failing`() {
        assertEquals(labelColorPair("slate"), labelColorPair("chartreuse"))
        assertNotEquals(labelColorPair("slate"), labelColorPair("red"))
    }
}
