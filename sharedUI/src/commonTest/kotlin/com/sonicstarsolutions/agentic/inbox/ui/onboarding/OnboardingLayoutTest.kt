package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The breakpoint decision is a pure function precisely so it can be tested here — the rest of the
 * screen's appearance would need Compose UI tests, which this repo doesn't have.
 */
class OnboardingLayoutTest {

    @Test
    fun `phone portrait is compact`() {
        assertEquals(OnboardingLayout.COMPACT, onboardingLayoutFor(maxWidth = 360.dp, maxHeight = 800.dp))
    }

    @Test
    fun `tablet portrait centres the form without a hero pane`() {
        assertEquals(OnboardingLayout.MEDIUM, onboardingLayoutFor(maxWidth = 800.dp, maxHeight = 1280.dp))
    }

    @Test
    fun `tablet landscape gets the hero pane`() {
        assertEquals(OnboardingLayout.EXPANDED, onboardingLayoutFor(maxWidth = 1280.dp, maxHeight = 800.dp))
    }

    @Test
    fun `a wide but short window drops the hero pane`() {
        // Phone in landscape: wide enough for EXPANDED on width alone, but a side-by-side hero
        // would leave the form squeezed into a few hundred dp of height.
        assertEquals(OnboardingLayout.MEDIUM, onboardingLayoutFor(maxWidth = 900.dp, maxHeight = 400.dp))
    }

    @Test
    fun `compact ends and medium begins at the 600dp window class boundary`() {
        assertEquals(OnboardingLayout.COMPACT, onboardingLayoutFor(maxWidth = 599.dp, maxHeight = 800.dp))
        assertEquals(OnboardingLayout.MEDIUM, onboardingLayoutFor(maxWidth = 600.dp, maxHeight = 800.dp))
    }

    @Test
    fun `medium ends and expanded begins at the 840dp window class boundary`() {
        assertEquals(OnboardingLayout.MEDIUM, onboardingLayoutFor(maxWidth = 839.dp, maxHeight = 800.dp))
        assertEquals(OnboardingLayout.EXPANDED, onboardingLayoutFor(maxWidth = 840.dp, maxHeight = 800.dp))
    }
}
