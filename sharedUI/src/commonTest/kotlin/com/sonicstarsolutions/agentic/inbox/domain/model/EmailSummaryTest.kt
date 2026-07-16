package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailSummaryTest {

    private fun summary(read: Boolean, threadUnreadCount: Int = 0) = EmailSummary(
        id = "e1",
        subject = "Subject",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        date = "2026-07-16T00:00:00Z",
        read = read,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        snippet = null,
        threadUnreadCount = threadUnreadCount,
    )

    @Test
    fun `read email with no unread thread messages is not unread`() {
        assertFalse(summary(read = true, threadUnreadCount = 0).isUnread())
    }

    @Test
    fun `unread email is unread regardless of thread count`() {
        assertTrue(summary(read = false, threadUnreadCount = 0).isUnread())
    }

    @Test
    fun `read email with unread thread messages is still unread`() {
        assertTrue(summary(read = true, threadUnreadCount = 2).isUnread())
    }
}
