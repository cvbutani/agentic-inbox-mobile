package com.sonicstarsolutions.agentic.inbox.testutil

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDraftRepository(
    initial: List<Draft> = emptyList(),
) : DraftRepository {
    private val rows = MutableStateFlow(initial)

    val saved: List<Draft> get() = rows.value
    val saveCalls = mutableListOf<Draft>()
    val deleteCalls = mutableListOf<String>()

    /** When set, [save] suspends here before recording — models the real DAO's write actually
     * taking time, which is what lets a test hold one save in flight while another starts. */
    var saveGate: CompletableDeferred<Unit>? = null

    override suspend fun save(draft: Draft) {
        saveCalls += draft
        saveGate?.await()
        rows.value = rows.value.filterNot { it.id == draft.id } + draft
    }

    override suspend fun get(id: String): Draft? = rows.value.firstOrNull { it.id == id }

    override fun observe(mailboxId: String): Flow<List<Draft>> =
        rows.map { list -> list.filter { it.mailboxId == mailboxId }.sortedByDescending { it.updatedAt } }

    override suspend fun delete(id: String) {
        deleteCalls += id
        rows.value = rows.value.filterNot { it.id == id }
    }
}
