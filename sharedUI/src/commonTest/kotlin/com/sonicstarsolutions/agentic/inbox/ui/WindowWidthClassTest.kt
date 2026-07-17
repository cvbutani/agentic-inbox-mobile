package com.sonicstarsolutions.agentic.inbox.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Material 3 width breakpoints (600dp, 840dp), factored out so every adaptive screen agrees
 * on where "phone" ends and "tablet" begins instead of each hand-rolling its own constant.
 */
class WindowWidthClassTest {

    @Test
    fun `phone portrait is compact`() {
        assertEquals(WindowWidthClass.COMPACT, windowWidthClassFor(360.dp))
    }

    @Test
    fun `tablet portrait is medium`() {
        assertEquals(WindowWidthClass.MEDIUM, windowWidthClassFor(800.dp))
    }

    @Test
    fun `tablet landscape is expanded`() {
        assertEquals(WindowWidthClass.EXPANDED, windowWidthClassFor(1280.dp))
    }

    @Test
    fun `compact ends and medium begins at the 600dp boundary`() {
        assertEquals(WindowWidthClass.COMPACT, windowWidthClassFor(599.dp))
        assertEquals(WindowWidthClass.MEDIUM, windowWidthClassFor(600.dp))
    }

    @Test
    fun `medium ends and expanded begins at the 840dp boundary`() {
        assertEquals(WindowWidthClass.MEDIUM, windowWidthClassFor(839.dp))
        assertEquals(WindowWidthClass.EXPANDED, windowWidthClassFor(840.dp))
    }
}
