package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils

/**
 * Dispatches a draft that's a real row on the server (sitting in the `draft` folder — see
 * [EmailDetail.inReplyTo]) exactly as its fields currently read, then removes that row so it
 * doesn't linger as a stale duplicate once the real message exists.
 *
 * [EmailDetail.inReplyTo] is what decides the send path, matching the Worker's own routes
 * (workers/routes/reply-forward.ts in cloudflare/agentic-inbox): a reply-in-progress carries the
 * original message's id there and must go through the reply endpoint targeting it, so the server
 * can derive correct threading headers itself — passing that id to a plain send would skip that
 * entirely and the reply would arrive as an unthreaded message.
 */
class SendDraftEmailUseCase(
    private val emailRepository: EmailRepository,
    private val mailboxRepository: MailboxRepository,
) {
    suspend operator fun invoke(mailboxId: String, draft: EmailDetail): Result<Unit> {
        val mailbox = mailboxRepository.getMailbox(mailboxId).getOrElse { return Result.failure(it) }
        val request = ComposeEmailRequest(
            fromEmail = mailbox.email,
            fromName = mailbox.name,
            to = EmailAddressUtils.parseAddressList(draft.recipient),
            cc = EmailAddressUtils.parseAddressList(draft.cc.orEmpty()),
            bcc = EmailAddressUtils.parseAddressList(draft.bcc.orEmpty()),
            subject = draft.subject,
            body = draft.body.orEmpty(),
        )
        val sendResult = draft.inReplyTo
            ?.let { originalEmailId -> emailRepository.replyEmail(mailboxId, originalEmailId, request) }
            ?: emailRepository.sendEmail(mailboxId, request)

        // Best-effort: the send already happened, so a cleanup failure here must not turn a real,
        // successful send into a reported failure. Worst case the stale row lingers for the user
        // to delete by hand.
        sendResult.onSuccess { emailRepository.deleteEmail(mailboxId, draft.id) }
        return sendResult
    }
}
