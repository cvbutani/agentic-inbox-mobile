package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository

class DeleteDraftUseCase(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
