package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class ForwardEmailUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> =
        repository.forwardEmail(mailboxId, emailId, request)
}
