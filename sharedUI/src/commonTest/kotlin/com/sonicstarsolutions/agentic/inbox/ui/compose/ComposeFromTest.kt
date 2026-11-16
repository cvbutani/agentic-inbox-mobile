package com.sonicstarsolutions.agentic.inbox.ui.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeFromTest {

    private val mailbox = "me@example.dev"
    private val aliases = listOf("sales@example.dev", "help@example.dev")

    @Test
    fun `mail sent to an alias is answered from that alias`() {
        assertEquals(
            "sales@example.dev",
            ComposeFrom.defaultFor(mailbox, aliases, addressedTo = listOf("Sales <sales@example.dev>")),
        )
    }

    @Test
    fun `an alias in cc counts as addressed to it`() {
        assertEquals("help@example.dev", ComposeFrom.defaultFor(mailbox, aliases, addressedTo = listOf("bob@x.dev", "HELP@example.dev")))
    }

    @Test
    fun `mail sent to the mailbox itself is answered from the mailbox`() {
        assertEquals(mailbox, ComposeFrom.defaultFor(mailbox, aliases, addressedTo = listOf("me@example.dev")))
    }

    @Test
    fun `nothing to go on means the mailbox address`() {
        assertEquals(mailbox, ComposeFrom.defaultFor(mailbox, aliases, addressedTo = emptyList()))
        assertEquals(mailbox, ComposeFrom.defaultFor(mailbox, emptyList(), addressedTo = listOf("sales@example.dev")))
    }

    @Test
    fun `the choices are the mailbox then its aliases, without repeats`() {
        assertEquals(
            listOf(mailbox, "sales@example.dev"),
            ComposeFrom.choices(mailbox, listOf("sales@example.dev", "SALES@example.dev", "me@example.dev")),
        )
    }

    @Test
    fun `the mailbox last sent from is the default when it still exists`() {
        assertEquals("b@x.dev", ComposeFrom.defaultMailbox(listOf("a@x.dev", "b@x.dev"), last = "b@x.dev"))
    }

    @Test
    fun `with nothing remembered the first mailbox alphabetically is the default`() {
        assertEquals("a@x.dev", ComposeFrom.defaultMailbox(listOf("b@x.dev", "a@x.dev"), last = null))
    }

    @Test
    fun `a remembered mailbox that no longer exists is ignored`() {
        assertEquals("a@x.dev", ComposeFrom.defaultMailbox(listOf("a@x.dev", "b@x.dev"), last = "gone@x.dev"))
    }

    @Test
    fun `remembered mailboxes match regardless of case`() {
        assertEquals("B@x.dev", ComposeFrom.defaultMailbox(listOf("a@x.dev", "B@x.dev"), last = "b@X.dev"))
    }

    @Test
    fun `no mailboxes means no default`() {
        assertEquals(null, ComposeFrom.defaultMailbox(emptyList(), last = "a@x.dev"))
    }
}
