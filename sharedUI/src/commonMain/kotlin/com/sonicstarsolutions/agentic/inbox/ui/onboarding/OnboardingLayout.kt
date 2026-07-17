package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.ui.WindowWidthClass
import com.sonicstarsolutions.agentic.inbox.ui.windowWidthClassFor

/** How much room the onboarding screen has, and therefore which shape it takes. */
enum class OnboardingLayout {
    /** Phone: the form fills the window. A card here would only waste width. */
    COMPACT,

    /** Tablet portrait: the form sits in a card, centred, capped at a readable width. */
    MEDIUM,

    /** Tablet landscape: a hero pane alongside the form card. */
    EXPANDED,
}

/** Below this the side-by-side hero would squeeze the form into too little height. */
private val HERO_MIN_HEIGHT = 480.dp

/**
 * Pure so it can be unit-tested — the alternative is asserting on window sizes through a Compose
 * UI test, which this repo has no infrastructure for.
 *
 * Starts from the shared [WindowWidthClass] boundaries, then layers on a rule specific to this
 * screen's own hero pane: a phone in landscape clears the EXPANDED width easily but has only a
 * few hundred dp of height, where a hero pane beside the form would leave the fields scrolling in
 * a sliver. Those windows get [OnboardingLayout.MEDIUM] instead — no other screen needs this
 * demotion, so it stays here rather than in the shared width-class primitive.
 */
fun onboardingLayoutFor(maxWidth: Dp, maxHeight: Dp): OnboardingLayout = when (windowWidthClassFor(maxWidth)) {
    WindowWidthClass.COMPACT -> OnboardingLayout.COMPACT
    WindowWidthClass.MEDIUM -> OnboardingLayout.MEDIUM
    WindowWidthClass.EXPANDED -> if (maxHeight < HERO_MIN_HEIGHT) OnboardingLayout.MEDIUM else OnboardingLayout.EXPANDED
}
