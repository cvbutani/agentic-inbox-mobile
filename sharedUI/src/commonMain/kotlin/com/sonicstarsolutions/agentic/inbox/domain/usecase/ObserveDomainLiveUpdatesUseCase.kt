package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * Change notices for a whole domain: one connection per mailbox on it, merged. The server has no
 * domain-wide socket, so a domain with more than [MAX_CONNECTIONS] mailboxes listens to the first
 * few only; the rest are picked up when the screen is next opened or refreshed.
 */
class ObserveDomainLiveUpdatesUseCase(
    private val getMailboxes: GetMailboxesUseCase,
    private val observeLiveUpdates: ObserveLiveUpdatesUseCase,
) {
    operator fun invoke(domain: String): Flow<LiveScope> = flow {
        val mailboxIds = getMailboxes().getOrNull().orEmpty()
            .filter { it.email.substringAfter('@').equals(domain, ignoreCase = true) }
            .take(MAX_CONNECTIONS)
            .map { it.id }
        emitAll(merge(*mailboxIds.map { observeLiveUpdates(it) }.toTypedArray()))
    }

    companion object {
        const val MAX_CONNECTIONS = 8
    }
}
