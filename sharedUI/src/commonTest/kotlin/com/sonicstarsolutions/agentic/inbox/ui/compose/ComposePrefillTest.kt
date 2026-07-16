package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposePrefillTest {

    private fun email(
        subject: String = "Hello",
        sender: String = "Alice <alice@example.dev>",
        recipient: String = "me@example.dev",
        cc: String? = null,
        body: String = "<p>Original body</p>",
    ) = EmailDetail(
        id = "e1",
        subject = subject,
        sender = sender,
        recipient = recipient,
        cc = cc,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        body = body,
        attachments = emptyList(),
    )

    @Test
    fun `forReply addresses the sender and leaves cc empty when not replying to all`() {
        val fields = ComposePrefill.forReply(email(), replyAll = false, ownEmail = "me@example.dev")

        assertEquals("alice@example.dev", fields.to)
        assertEquals("", fields.cc)
    }

    @Test
    fun `forReply prefixes a subject with Re when not already prefixed`() {
        val fields = ComposePrefill.forReply(email(subject = "Hello"), replyAll = false, ownEmail = "me@example.dev")

        assertEquals("Re: Hello", fields.subject)
    }

    @Test
    fun `forReply does not double-prefix a subject that already says Re`() {
        val fields = ComposePrefill.forReply(email(subject = "Re: Hello"), replyAll = false, ownEmail = "me@example.dev")

        assertEquals("Re: Hello", fields.subject)
    }

    @Test
    fun `forReply quotes the plain text of the original body`() {
        val fields = ComposePrefill.forReply(email(body = "<p>Hi <b>there</b></p>"), replyAll = false, ownEmail = "me@example.dev")

        assertTrue(fields.body.contains("Hi there"), fields.body)
        assertTrue(fields.body.contains("Alice <alice@example.dev>"), fields.body)
    }

    @Test
    fun `forReply with replyAll ccs the other recipients but excludes the sender and own address`() {
        val original = email(
            sender = "Alice <alice@example.dev>",
            recipient = "me@example.dev, Bob <bob@example.dev>",
            cc = "Carol <carol@example.dev>",
        )

        val fields = ComposePrefill.forReply(original, replyAll = true, ownEmail = "me@example.dev")

        assertEquals("alice@example.dev", fields.to)
        assertEquals("bob@example.dev, carol@example.dev", fields.cc)
    }

    @Test
    fun `forReply on a message the mailbox itself sent addresses the original recipient instead`() {
        val original = email(
            sender = "Me <me@example.dev>",
            recipient = "Alice <alice@example.dev>",
        )

        val fields = ComposePrefill.forReply(original, replyAll = false, ownEmail = "me@example.dev")

        assertEquals("alice@example.dev", fields.to)
    }

    @Test
    fun `forReply with replyAll on a self-sent message addresses all original recipients and ccs the rest`() {
        val original = email(
            sender = "Me <me@example.dev>",
            recipient = "Alice <alice@example.dev>, Bob <bob@example.dev>",
            cc = "Carol <carol@example.dev>",
        )

        val fields = ComposePrefill.forReply(original, replyAll = true, ownEmail = "me@example.dev")

        assertEquals("alice@example.dev, bob@example.dev", fields.to)
        assertEquals("carol@example.dev", fields.cc)
    }

    @Test
    fun `forForward leaves the recipient blank for the user to fill in`() {
        val fields = ComposePrefill.forForward(email())

        assertEquals("", fields.to)
        assertEquals("", fields.cc)
    }

    @Test
    fun `forForward prefixes a subject with Fwd when not already prefixed`() {
        val fields = ComposePrefill.forForward(email(subject = "Hello"))

        assertEquals("Fwd: Hello", fields.subject)
    }

    @Test
    fun `forForward does not double-prefix a subject that already says Fwd`() {
        val fields = ComposePrefill.forForward(email(subject = "Fwd: Hello"))

        assertEquals("Fwd: Hello", fields.subject)
    }

    @Test
    fun `forForward quotes the plain text of the original body`() {
        val fields = ComposePrefill.forForward(email(body = "<p>Hi <b>there</b></p>"))

        assertTrue(fields.body.contains("Hi there"), fields.body)
    }
}
