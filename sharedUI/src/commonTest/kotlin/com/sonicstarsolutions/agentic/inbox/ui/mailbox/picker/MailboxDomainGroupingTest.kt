package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import kotlin.test.Test
import kotlin.test.assertEquals

class MailboxDomainGroupingTest {

    private fun mailbox(email: String, id: String = email) =
        Mailbox(id = id, email = email, name = email)

    @Test
    fun `groups mailboxes under their domain`() {
        val mailboxes = listOf(
            mailbox("alice@work.com"),
            mailbox("bob@home.com"),
            mailbox("carol@work.com"),
        )

        val grouped = groupMailboxesByDomain(mailboxes)

        assertEquals(
            listOf(
                "home.com" to listOf(mailboxes[1]),
                "work.com" to listOf(mailboxes[0], mailboxes[2]),
            ),
            grouped,
        )
    }

    @Test
    fun `sorts domains alphabetically regardless of input order`() {
        val mailboxes = listOf(mailbox("z@zeta.com"), mailbox("a@alpha.com"))

        val grouped = groupMailboxesByDomain(mailboxes)

        assertEquals(listOf("alpha.com", "zeta.com"), grouped.map { it.first })
    }

    @Test
    fun `groups domains case-insensitively`() {
        val mailboxes = listOf(mailbox("a@Work.com"), mailbox("b@work.com"))

        val grouped = groupMailboxesByDomain(mailboxes)

        assertEquals(1, grouped.size)
        assertEquals("work.com", grouped.single().first)
        assertEquals(2, grouped.single().second.size)
    }

    @Test
    fun `empty list produces no groups`() {
        assertEquals(emptyList(), groupMailboxesByDomain(emptyList()))
    }
}
