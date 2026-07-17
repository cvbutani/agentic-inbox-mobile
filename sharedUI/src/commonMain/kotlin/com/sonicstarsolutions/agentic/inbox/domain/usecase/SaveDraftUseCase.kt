package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository

class SaveDraftUseCase(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(draft: Draft) = repository.save(draft)
}
