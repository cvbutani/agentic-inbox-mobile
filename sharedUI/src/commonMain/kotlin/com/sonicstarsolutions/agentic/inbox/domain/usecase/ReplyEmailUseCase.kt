package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class ReplyEmailUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> =
        repository.replyEmail(mailboxId, emailId, request)
}
