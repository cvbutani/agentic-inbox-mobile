package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class MarkThreadReadUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, threadId: String): Result<Unit> =
        repository.markThreadRead(mailboxId, threadId)
}
