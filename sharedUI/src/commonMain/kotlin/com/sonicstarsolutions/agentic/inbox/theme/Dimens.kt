package com.sonicstarsolutions.agentic.inbox.theme

import androidx.compose.ui.unit.dp

/** Shared list-row grid from docs/DESIGN_SYSTEM.md — every email/draft row, its skeleton
 * placeholder, and the divider under it must agree on these or the 72dp text edge drifts. */
object ListRowDimens {
    val horizontalPadding = 16.dp
    val verticalPadding = 12.dp
    val avatarSize = 40.dp
    val contentGap = 16.dp
    val lineGap = 2.dp
    val trailingGap = 8.dp

    /** Leading slot + gutter + row padding — where every row's text starts, and where
     * HorizontalDivider insets to, on both inbox and search lists. */
    val textEdge = horizontalPadding + avatarSize + contentGap
}

/** Accessibility floor from docs/DESIGN_SYSTEM.md — touch targets never shrink below this. */
val MinTouchTarget = 48.dp
