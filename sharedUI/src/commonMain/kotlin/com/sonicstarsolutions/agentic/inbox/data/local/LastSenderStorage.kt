package com.sonicstarsolutions.agentic.inbox.data.local

import com.sonicstarsolutions.agentic.inbox.domain.repository.LastSenderStore
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.serialization.Serializable

/** The persisted shape: domain (lowercase) to the mailbox last sent from. */
@Serializable
data class LastSenders(val byDomain: Map<String, String> = emptyMap())

class KSafeLastSenderStore(private val ksafe: KSafe) : LastSenderStore {
    companion object {
        private const val KEY_LAST_SENDERS = "last_senders"
    }

    override suspend fun lastMailbox(domain: String): String? =
        ksafe.get(KEY_LAST_SENDERS, LastSenders()).byDomain[domain.lowercase()]

    override suspend fun remember(domain: String, mailboxId: String) {
        val current = ksafe.get(KEY_LAST_SENDERS, LastSenders())
        ksafe.put(KEY_LAST_SENDERS, current.copy(byDomain = current.byDomain + (domain.lowercase() to mailboxId)))
    }
}
