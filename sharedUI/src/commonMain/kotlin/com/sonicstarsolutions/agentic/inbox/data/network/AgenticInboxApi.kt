package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.data.network.dto.ConfigDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto

/**
 * API client for the agentic-inbox Worker. M0 ships the endpoints required for
 * onboarding validation and the mailbox picker; the rest land in M1+.
 *
 * All endpoints are relative to [baseUrl]; the impl stitches credentials via
 * an internal interceptor and assumes the Access service-token auth model
 * (Option A from the plan).
 */
interface AgenticInboxApi {
    suspend fun getConfig(): ConfigDto
    suspend fun listMailboxes(): List<MailboxDto>
    suspend fun createMailbox(body: CreateMailboxDto): MailboxDto
}
