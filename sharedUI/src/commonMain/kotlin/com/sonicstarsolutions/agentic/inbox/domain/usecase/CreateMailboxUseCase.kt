package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class CreateMailboxUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(email: String, name: String): Result<Mailbox> =
        repository.createMailbox(email, name)
}
