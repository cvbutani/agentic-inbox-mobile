package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow

class ObserveDraftsUseCase(
    private val repository: DraftRepository,
) {
    operator fun invoke(mailboxId: String): Flow<List<Draft>> = repository.observe(mailboxId)
}
