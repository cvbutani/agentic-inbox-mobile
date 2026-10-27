package com.sonicstarsolutions.agentic.inbox.ui.mailbox.agentaccess

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope

internal val AgentScope.label: String
    get() = when (this) {
        AgentScope.READ -> "Read"
        AgentScope.DRAFT -> "Draft"
        AgentScope.SEND -> "Send"
    }

internal val AgentScope.description: String
    get() = when (this) {
        AgentScope.READ -> "List, open and search mail"
        AgentScope.DRAFT -> "Create and edit drafts"
        AgentScope.SEND -> "Send email and replies"
    }

/** One line for a key row: what it may do, and its daily cap if it can send. */
internal fun agentKeySummary(key: AgentKey): String {
    if (key.scopes.isEmpty()) return "No permissions this app recognises"
    val permissions = key.scopes.joinToString(" · ") { it.label }
    val limit = key.dailySendLimit?.takeIf { AgentScope.SEND in key.scopes } ?: return permissions
    return "$permissions · up to $limit ${if (limit == 1) "send" else "sends"} a day"
}
