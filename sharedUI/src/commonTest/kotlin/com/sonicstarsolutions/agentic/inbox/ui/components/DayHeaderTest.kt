package com.sonicstarsolutions.agentic.inbox.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DayHeaderTest {

    @Test
    fun `no unread mail shows no count`() {
        assertNull(unreadLabel(0))
        assertNull(unreadLabel(-1))
    }

    @Test
    fun `a count shows as plain text`() {
        assertEquals("1 unread", unreadLabel(1))
        assertEquals("3 unread", unreadLabel(3))
        assertEquals("99 unread", unreadLabel(99))
    }

    @Test
    fun `a count above 99 is capped`() {
        assertEquals("99+ unread", unreadLabel(100))
        assertEquals("99+ unread", unreadLabel(4200))
    }

    @Test
    fun `postmark month is three uppercase letters`() {
        assertEquals("JAN", postmarkMonth(kotlinx.datetime.LocalDate(2026, 1, 5)))
        assertEquals("DEC", postmarkMonth(kotlinx.datetime.LocalDate(2026, 12, 31)))
    }
}
