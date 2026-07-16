package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class SendEmailUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, request: ComposeEmailRequest): Result<Unit> =
        repository.sendEmail(mailboxId, request)
}
