package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import kotlinx.coroutines.flow.Flow

/**
 * Local-only store for unsent messages — no network path exists for drafts (see [Draft]), so
 * unlike the other repositories this one never returns [Result]: there's nothing to fail but the
 * local database.
 */
interface DraftRepository {
    suspend fun save(draft: Draft)
    suspend fun get(id: String): Draft?
    fun observe(mailboxId: String): Flow<List<Draft>>
    suspend fun delete(id: String)
}
