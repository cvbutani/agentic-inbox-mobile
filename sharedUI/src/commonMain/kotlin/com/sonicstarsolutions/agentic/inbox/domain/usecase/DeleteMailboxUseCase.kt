package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class DeleteMailboxUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(mailboxId: String): Result<Unit> = repository.deleteMailbox(mailboxId)
}
