package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveCredentialsUseCase(
    private val repository: CredentialsRepository,
) {
    operator fun invoke(): StateFlow<Credentials> = repository.state
}
