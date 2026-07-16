package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class SetEmailStarredUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, starred: Boolean): Result<Unit> =
        repository.setStarred(mailboxId, emailId, starred)
}
