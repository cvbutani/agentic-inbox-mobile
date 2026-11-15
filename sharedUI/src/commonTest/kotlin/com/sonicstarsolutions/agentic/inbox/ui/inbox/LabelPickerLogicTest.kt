package com.sonicstarsolutions.agentic.inbox.ui.inbox

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelPickerLogicTest {

    @Test
    fun `a tap turns a missing or mixed label fully on, and a full one off`() {
        assertEquals(LabelCheck.ALL, nextCheck(LabelCheck.NONE))
        assertEquals(LabelCheck.ALL, nextCheck(LabelCheck.SOME))
        assertEquals(LabelCheck.NONE, nextCheck(LabelCheck.ALL))
    }

    @Test
    fun `only labels the user changed are sent`() {
        val start = mapOf("a" to LabelCheck.NONE, "b" to LabelCheck.ALL, "c" to LabelCheck.SOME, "d" to LabelCheck.NONE)
        val end = mapOf("a" to LabelCheck.ALL, "b" to LabelCheck.NONE, "c" to LabelCheck.SOME, "d" to LabelCheck.NONE)

        assertEquals(mapOf("a" to true, "b" to false), labelChanges(start, end))
    }

    @Test
    fun `a mixed label that was tapped on resolves to on`() {
        val start = mapOf("c" to LabelCheck.SOME)

        assertEquals(mapOf("c" to true), labelChanges(start, mapOf("c" to nextCheck(LabelCheck.SOME))))
    }

    @Test
    fun `tapping twice back to where it began is not a change`() {
        val start = mapOf("a" to LabelCheck.NONE)

        assertEquals(emptyMap(), labelChanges(start, mapOf("a" to nextCheck(nextCheck(LabelCheck.NONE)))))
    }
}
