package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.data.network.dto.ConfigDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor-backed implementation of [AgenticInboxApi]. Built over the common
 * base URL carried by [HttpClient]'s defaultRequest.
 */
internal class KtorAgenticInboxApi(
    private val client: HttpClient,
) : AgenticInboxApi {
    override suspend fun getConfig(): ConfigDto =
        client.get("/api/v1/config").body()

    override suspend fun listMailboxes(): List<MailboxDto> =
        client.get("/api/v1/mailboxes").body()

    override suspend fun createMailbox(body: CreateMailboxDto): MailboxDto =
        client.post("/api/v1/mailboxes") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
}
