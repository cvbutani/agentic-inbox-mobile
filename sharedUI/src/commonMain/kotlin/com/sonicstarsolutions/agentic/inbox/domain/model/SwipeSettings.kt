package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlinx.serialization.Serializable

/** What a swipe on an inbox row can do. Order is the order the settings picker lists them in. */
@Serializable
enum class SwipeAction(
    /** The name in the settings picker, where there is no particular email to describe. */
    val settingLabel: String,
) {
    Archive("Archive"),
    Delete("Delete"),
    ToggleRead("Mark read / unread"),
    ToggleStar("Star / unstar"),
    None("Nothing"),
    ;

    /** The word shown behind a row while it is dragged — says what *this* swipe will do to *this* row. */
    fun labelFor(email: EmailSummary): String = when (this) {
        Archive -> "Archive"
        Delete -> "Delete"
        ToggleRead -> if (email.isUnread()) "Mark read" else "Mark unread"
        ToggleStar -> if (email.starred) "Unstar" else "Star"
        None -> ""
    }
}

/**
 * The user's choice of swipe actions. Local to this device, not part of any mailbox's server-side
 * settings — hence also the KSafe-persisted shape (@Serializable), like [Credentials].
 */
@Serializable
data class SwipeSettings(
    val swipeRight: SwipeAction = SwipeAction.Archive,
    val swipeLeft: SwipeAction = SwipeAction.Delete,
)
