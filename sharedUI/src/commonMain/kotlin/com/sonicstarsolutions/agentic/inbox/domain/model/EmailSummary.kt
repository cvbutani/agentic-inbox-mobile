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
) {
    fun isUnread(): Boolean = !read || threadUnreadCount > 0
}
