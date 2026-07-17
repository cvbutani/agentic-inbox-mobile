package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A draft here means a real row on the server sitting in the `draft` folder (see
 * cloudflare/agentic-inbox's workers/index.ts /drafts handler) — not this app's own local-only
 * Draft (Room) feature. Sending one means dispatching its current fields for real and then
 * removing the stale draft row, so the same message doesn't end up duplicated afterward.
 */
class SendDraftEmailUseCaseTest {

    private fun draft(
        id: String = "d1",
        inReplyTo: String? = null,
        recipient: String = "bob@example.dev",
        cc: String? = null,
        bcc: String? = null,
        subject: String = "Re: Hello",
        body: String? = "Draft body",
    ) = EmailDetail(
        id = id,
        subject = subject,
        sender = "me@example.dev",
        recipient = recipient,
        cc = cc,
        bcc = bcc,
        date = "2026-07-17T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = "draft",
        body = body,
        attachments = emptyList<EmailAttachment>(),
        inReplyTo = inReplyTo,
    )

    private fun buildUseCase(
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        },
    ) = SendDraftEmailUseCase(emailRepository, mailboxRepository)

    @Test
    fun `a draft with inReplyTo sends via replyEmail targeting the original message`() = runTest {
        val emailRepository = FakeEmailRepository()
        val useCase = buildUseCase(emailRepository)

        val result = useCase("mb1", draft(inReplyTo = "original1"))

        assertTrue(result.isSuccess)
        val call = emailRepository.replyCalls.single()
        assertEquals("mb1", call.mailboxId)
        assertEquals("original1", call.emailId, "must target the original message id, not the draft's own id")
        assertEquals("me@example.dev", call.request.fromEmail)
        assertEquals("Me", call.request.fromName)
        assertEquals(listOf("bob@example.dev"), call.request.to)
        assertEquals("Re: Hello", call.request.subject)
        assertEquals("Draft body", call.request.body)
        assertTrue(emailRepository.sendCalls.isEmpty(), "should not also hit the plain send endpoint")
    }

    @Test
    fun `a draft without inReplyTo sends via plain sendEmail`() = runTest {
        val emailRepository = FakeEmailRepository()
        val useCase = buildUseCase(emailRepository)

        val result = useCase("mb1", draft(inReplyTo = null))

        assertTrue(result.isSuccess)
        assertEquals(1, emailRepository.sendCalls.size)
        assertTrue(emailRepository.replyCalls.isEmpty(), "should not hit the reply endpoint with no original to target")
    }

    @Test
    fun `cc and bcc on the draft carry through to the outgoing request`() = runTest {
        val emailRepository = FakeEmailRepository()
        val useCase = buildUseCase(emailRepository)

        useCase("mb1", draft(inReplyTo = "original1", cc = "carol@example.dev", bcc = "dave@example.dev"))

        val call = emailRepository.replyCalls.single()
        assertEquals(listOf("carol@example.dev"), call.request.cc)
        assertEquals(listOf("dave@example.dev"), call.request.bcc)
    }

    @Test
    fun `a successful send deletes the stale draft row`() = runTest {
        val emailRepository = FakeEmailRepository()
        val useCase = buildUseCase(emailRepository)

        useCase("mb1", draft(id = "d1", inReplyTo = "original1"))

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "d1")), emailRepository.deleteCalls)
    }

    @Test
    fun `a failed send does not attempt to delete the draft`() = runTest {
        val emailRepository = FakeEmailRepository(replyResult = Result.failure(RuntimeException("offline")))
        val useCase = buildUseCase(emailRepository)

        val result = useCase("mb1", draft(inReplyTo = "original1"))

        assertTrue(result.isFailure)
        assertTrue(emailRepository.deleteCalls.isEmpty(), "nothing was sent, so the draft must survive")
    }

    @Test
    fun `the draft survives if the delete cleanup itself fails, since the send already succeeded`() = runTest {
        val emailRepository = FakeEmailRepository(deleteResult = Result.failure(RuntimeException("not found")))
        val useCase = buildUseCase(emailRepository)

        val result = useCase("mb1", draft(inReplyTo = "original1"))

        assertTrue(result.isSuccess, "a failed best-effort cleanup must not turn a real, successful send into a reported failure")
    }

    @Test
    fun `a failed mailbox lookup surfaces as a failure without sending anything`() = runTest {
        val emailRepository = FakeEmailRepository()
        val mailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.failure(RuntimeException("mailbox gone")) }
        }
        val useCase = buildUseCase(emailRepository, mailboxRepository)

        val result = useCase("mb1", draft(inReplyTo = "original1"))

        assertTrue(result.isFailure)
        assertTrue(emailRepository.replyCalls.isEmpty())
        assertTrue(emailRepository.sendCalls.isEmpty())
    }
}
