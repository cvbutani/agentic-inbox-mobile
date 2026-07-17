package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How much room the onboarding screen has, and therefore which shape it takes. */
enum class OnboardingLayout {
    /** Phone: the form fills the window. A card here would only waste width. */
    COMPACT,

    /** Tablet portrait: the form sits in a card, centred, capped at a readable width. */
    MEDIUM,

    /** Tablet landscape: a hero pane alongside the form card. */
    EXPANDED,
}

/** Material 3 window size class boundaries. */
private val MEDIUM_MIN_WIDTH = 600.dp
private val EXPANDED_MIN_WIDTH = 840.dp

/** Below this the side-by-side hero would squeeze the form into too little height. */
private val HERO_MIN_HEIGHT = 480.dp

/**
 * Pure so it can be unit-tested — the alternative is asserting on window sizes through a Compose
 * UI test, which this repo has no infrastructure for.
 *
 * Height matters as well as width: a phone in landscape clears [EXPANDED_MIN_WIDTH] easily but has
 * only a few hundred dp of height, where a hero pane beside the form would leave the fields
 * scrolling in a sliver. Those windows get [MEDIUM] instead.
 */
fun onboardingLayoutFor(maxWidth: Dp, maxHeight: Dp): OnboardingLayout = when {
    maxWidth < MEDIUM_MIN_WIDTH -> OnboardingLayout.COMPACT
    maxWidth < EXPANDED_MIN_WIDTH || maxHeight < HERO_MIN_HEIGHT -> OnboardingLayout.MEDIUM
    else -> OnboardingLayout.EXPANDED
}
