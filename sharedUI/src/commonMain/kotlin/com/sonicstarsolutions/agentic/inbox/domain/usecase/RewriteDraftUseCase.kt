package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class RewriteDraftUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(mailboxId: String, text: String, tone: String = "fix"): Result<String> =
        repository.rewriteDraft(mailboxId, text, tone)
}
