package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository

class SaveCredentialsUseCase(
    private val repository: CredentialsRepository,
) {
    suspend operator fun invoke(credentials: Credentials) = repository.save(credentials)
}
