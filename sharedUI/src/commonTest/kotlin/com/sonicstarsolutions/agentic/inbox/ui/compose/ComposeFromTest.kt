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
}
