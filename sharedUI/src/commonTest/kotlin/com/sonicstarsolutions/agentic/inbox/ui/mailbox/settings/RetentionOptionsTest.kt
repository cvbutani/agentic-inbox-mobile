package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class RetentionOptionsTest {

    @Test
    fun `days read as words`() {
        assertEquals("Never", retentionLabel(0))
        assertEquals("1 day", retentionLabel(1))
        assertEquals("7 days", retentionLabel(7))
        assertEquals("30 days", retentionLabel(30))
        assertEquals("1 year", retentionLabel(365))
        assertEquals("2 years", retentionLabel(730))
        assertEquals("400 days", retentionLabel(400))
    }

    @Test
    fun `the presets run shortest to longest with never last`() {
        assertEquals(listOf(7, 30, 90, 180, 365, 0), retentionChoices(current = null))
    }

    @Test
    fun `a custom value set elsewhere is offered too so it never disappears from the list`() {
        assertEquals(listOf(7, 30, 45, 90, 180, 365, 0), retentionChoices(current = 45))
        assertEquals(listOf(7, 30, 90, 180, 365, 0), retentionChoices(current = 90))
    }
}
