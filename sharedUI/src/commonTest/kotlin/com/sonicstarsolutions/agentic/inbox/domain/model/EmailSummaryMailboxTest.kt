package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EmailSummaryMailboxTest {

    private fun email(id: String, mailboxId: String) = EmailSummary(
        id = id, subject = "s", sender = "a", recipient = "b", date = "2026-07-10T10:00:00Z",
        read = false, starred = false, threadId = null, folderId = "inbox", snippet = null, mailboxId = mailboxId,
    )

    @Test
    fun `the short name is the address before the at sign`() {
        assertEquals("alice", email("e1", "alice@example.com").mailboxShortName)
        assertEquals("sales.team", email("e1", "sales.team@example.com").mailboxShortName)
    }

    @Test
    fun `a row with no mailbox has a blank short name`() {
        assertEquals("", email("e1", "").mailboxShortName)
    }

    @Test
    fun `list keys differ for the same id in different mailboxes`() {
        assertNotEquals(email("e1", "a@example.com").listKey, email("e1", "b@example.com").listKey)
    }

    @Test
    fun `a single-mailbox row keeps its plain id as its key`() {
        assertEquals("e1", email("e1", "").listKey)
    }
}
