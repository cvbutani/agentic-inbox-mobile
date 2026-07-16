package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository

class ValidateConnectionUseCase(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.validate()
}
