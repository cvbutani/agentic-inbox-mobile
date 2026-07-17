package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository

class GetDraftUseCase(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(id: String): Draft? = repository.get(id)
}
