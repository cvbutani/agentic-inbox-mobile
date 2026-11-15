package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import com.sonicstarsolutions.agentic.inbox.domain.repository.LiveUpdates
import kotlinx.coroutines.flow.Flow

class ObserveLiveUpdatesUseCase(private val liveUpdates: LiveUpdates) {
    operator fun invoke(mailboxId: String): Flow<LiveScope> = liveUpdates.changes(mailboxId)
}
