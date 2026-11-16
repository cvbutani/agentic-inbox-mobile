package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

/** The retention periods offered in settings, shortest first; 0 means keep forever and goes last. */
private val RETENTION_PRESETS = listOf(7, 30, 90, 180, 365)

/** "Never", "1 day", "30 days", "1 year", "2 years", or plain days when it isn't a whole number of years. */
internal fun retentionLabel(days: Int): String = when {
    days == 0 -> "Never"
    days == 1 -> "1 day"
    days % 365 == 0 -> if (days == 365) "1 year" else "${days / 365} years"
    else -> "$days days"
}

/**
 * The periods to offer: the presets, plus [current] when it is something else (set from the web
 * app, say) so the picker can always show what is in force. "Never" is last.
 */
internal fun retentionChoices(current: Int?): List<Int> {
    val days = (RETENTION_PRESETS + listOfNotNull(current?.takeIf { it > 0 })).distinct().sorted()
    return days + 0
}
