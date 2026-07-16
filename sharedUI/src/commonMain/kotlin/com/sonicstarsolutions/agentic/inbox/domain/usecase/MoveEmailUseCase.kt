package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class MoveEmailUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, folderId: String): Result<Unit> =
        repository.moveEmail(mailboxId, emailId, folderId)
}
