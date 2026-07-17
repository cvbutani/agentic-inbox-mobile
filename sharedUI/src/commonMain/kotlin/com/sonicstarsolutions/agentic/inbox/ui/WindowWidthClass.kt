package com.sonicstarsolutions.agentic.inbox.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3's width breakpoints — the shared primitive every adaptive screen in this app
 * (Onboarding, Mailbox picker, Inbox's list-detail split) measures itself against, so "phone" vs
 * "tablet" means the same window width everywhere instead of each screen picking its own cutoff.
 */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

private val MEDIUM_MIN_WIDTH = 600.dp
private val EXPANDED_MIN_WIDTH = 840.dp

/** Pure so it's unit-testable without Compose UI test infrastructure, which this repo doesn't have. */
fun windowWidthClassFor(maxWidth: Dp): WindowWidthClass = when {
    maxWidth < MEDIUM_MIN_WIDTH -> WindowWidthClass.COMPACT
    maxWidth < EXPANDED_MIN_WIDTH -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}
