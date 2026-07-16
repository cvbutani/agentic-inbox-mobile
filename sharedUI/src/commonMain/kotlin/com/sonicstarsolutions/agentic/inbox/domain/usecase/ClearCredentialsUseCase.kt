package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository

class ClearCredentialsUseCase(
    private val repository: CredentialsRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
