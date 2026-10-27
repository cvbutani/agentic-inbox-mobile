package com.sonicstarsolutions.agentic.inbox.domain.model

/** How a mark/unmark-as-spam request was satisfied, so the UI can tell the user the truth. */
enum class MarkSpamOutcome {
    /** The Worker's spam endpoint handled it. */
    MARKED,

    /** The Worker has no spam endpoint (older version, or AI unavailable), so the email was
     * moved to Trash (or back to the Inbox when un-spamming) instead. */
    FALLBACK_MOVED,
}
