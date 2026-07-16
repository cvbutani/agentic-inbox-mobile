package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class GetMailboxUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(mailboxId: String): Result<Mailbox> = repository.getMailbox(mailboxId)
}
