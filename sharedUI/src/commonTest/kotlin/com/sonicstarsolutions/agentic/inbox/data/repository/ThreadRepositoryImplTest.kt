package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadRepositoryImplTest {

    private fun apiFor(engine: MockEngine): AgenticInboxApi {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false })
            }
        }
        return Ktorfit.Builder()
            .baseUrl("https://my-worker.example.dev", checkUrl = false)
            .httpClient(client)
            .build()
            .createAgenticInboxApi()
    }

    private val emailJson = """
        {
            "id": "e1",
            "subject": "Hello",
            "sender": "a@example.dev",
            "recipient": "b@example.dev",
            "cc": null,
            "bcc": null,
            "date": "2026-07-16T00:00:00Z",
            "read": true,
            "starred": false,
            "thread_id": "t1",
            "folder_id": "inbox",
            "body": "<p>Hi</p>",
            "attachments": [
                {"id": "a1", "filename": "invoice.pdf", "mimetype": "application/pdf", "size": 1024}
            ]
        }
    """.trimIndent()

    private val expectedDetail = EmailDetail(
        id = "e1",
        subject = "Hello",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        body = "<p>Hi</p>",
        attachments = listOf(EmailAttachment(id = "a1", filename = "invoice.pdf", mimetype = "application/pdf", size = 1024)),
    )

    @Test
    fun `getThread with a threadId fetches the whole thread`() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = "[$emailJson]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = ThreadRepositoryImpl(apiFor(engine))

        val result = repository.getThread(mailboxId = "mb1", emailId = "e1", threadId = "t1")

        assertEquals(listOf(expectedDetail), result.getOrThrow())
        assertEquals("/api/v1/mailboxes/mb1/threads/t1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `getThread with no threadId fetches just the single email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = emailJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = ThreadRepositoryImpl(apiFor(engine))

        val result = repository.getThread(mailboxId = "mb1", emailId = "e1", threadId = null)

        assertEquals(listOf(expectedDetail), result.getOrThrow())
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `getThread surfaces failures instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = ThreadRepositoryImpl(apiFor(engine))

        val result = repository.getThread(mailboxId = "mb1", emailId = "e1", threadId = null)

        assertTrue(result.isFailure)
    }
}
