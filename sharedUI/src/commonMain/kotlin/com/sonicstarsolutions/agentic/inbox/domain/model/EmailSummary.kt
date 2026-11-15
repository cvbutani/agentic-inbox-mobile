package com.sonicstarsolutions.agentic.inbox.domain.model

data class EmailSummary(
    val id: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val date: String,
    val read: Boolean,
    val starred: Boolean,
    val threadId: String?,
    val folderId: String?,
    val snippet: String?,
    // The list row's own `read` reflects just this one message — a thread can still have earlier
    // unread messages in it even once its latest message is read, so the list must check both.
    val threadUnreadCount: Int = 0,
    val labels: List<Label> = emptyList(),
    /** How many messages the row stands for: more than 1 means it is a whole conversation. */
    val threadCount: Int = 1,
    /** The mailbox this row lives in. In a single mailbox's list it is that mailbox; in the
     * domain-wide "All inboxes" list it is what says where to send a swipe, a reply or a delete. */
    val mailboxId: String = "",
) {
    fun isUnread(): Boolean = !read || threadUnreadCount > 0
}

/** True when this row is a whole conversation (several messages), not a single message. */
val EmailSummary.isConversation: Boolean get() = threadCount > 1 && !threadId.isNullOrBlank()

/** The part of the mailbox address before the @ — enough to tell which of a domain's mailboxes a
 * row lives in when they are listed together. Blank when the row has no mailbox of its own. */
val EmailSummary.mailboxShortName: String get() = mailboxId.substringBefore('@')

/** A key unique across a domain's mailboxes: two of them can each hold a message with the same id. */
val EmailSummary.listKey: String get() = if (mailboxId.isBlank()) id else "$mailboxId/$id"
