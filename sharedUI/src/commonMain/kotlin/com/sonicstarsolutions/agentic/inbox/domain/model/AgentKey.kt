package com.sonicstarsolutions.agentic.inbox.domain.model

/** What an agent key may do on its mailbox. Declaration order is the order shown and sent. */
enum class AgentScope(val wireName: String) {
    READ("read"),
    DRAFT("draft"),
    SEND("send"),
    ;

    companion object {
        /** Null for a scope a newer Worker introduced — callers skip it rather than fail. */
        fun fromWireName(name: String): AgentScope? = entries.firstOrNull { it.wireName == name }
    }
}

/** A key as the Worker lists it. The secret is never part of this — it exists only in
 * [CreatedAgentKey], at the moment of creation. */
data class AgentKey(
    val id: String,
    /** Non-secret handle ("agt_<id>") for recognising a key in a list. */
    val prefix: String,
    val name: String,
    val scopes: List<AgentScope>,
    /** Sends allowed per UTC day; null when the key can't send or has no cap. */
    val dailySendLimit: Int?,
    val createdAt: String,
    val revokedAt: String?,
) {
    val revoked: Boolean get() = revokedAt != null
}

data class NewAgentKey(
    val name: String,
    val scopes: List<AgentScope>,
    val dailySendLimit: Int?,
)

/** The one moment the secret is available: the response to creating a key. */
data class CreatedAgentKey(
    val key: AgentKey,
    val secret: String,
)
