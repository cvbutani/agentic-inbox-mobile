package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import kotlinx.coroutines.flow.Flow

interface LiveUpdates {
    /**
     * A notice each time something in [mailboxId] changes on the server. The connection is open
     * only while the flow is being collected and reopens by itself if it drops; it never
     * completes and never throws, so a server without live updates just produces no notices.
     */
    fun changes(mailboxId: String): Flow<LiveScope>
}
