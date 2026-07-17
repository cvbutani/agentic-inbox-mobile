package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers this screen's own composition on top of the shared width-class boundaries (see
 * WindowWidthClassTest for those) — specifically the hero pane's height-based demotion, which is
 * unique to this screen.
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
    fun `the hero pane's height threshold sits right at the expanded width boundary`() {
        // Confirms the demotion is layered onto the shared EXPANDED boundary (840dp) rather than
        // some other width — at 839dp this is already MEDIUM regardless of height.
        assertEquals(OnboardingLayout.MEDIUM, onboardingLayoutFor(maxWidth = 840.dp, maxHeight = 479.dp))
        assertEquals(OnboardingLayout.EXPANDED, onboardingLayoutFor(maxWidth = 840.dp, maxHeight = 480.dp))
    }
}
