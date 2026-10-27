package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class MarkEmailSpamUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, spam: Boolean): Result<Unit> =
        repository.markSpam(mailboxId, emailId, spam)
}
