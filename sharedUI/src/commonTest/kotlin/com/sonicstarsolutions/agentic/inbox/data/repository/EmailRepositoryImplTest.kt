package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
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

class EmailRepositoryImplTest {

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

    @Test
    fun `getEmails requests the given mailbox folder and page`() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """{"emails":[],"totalCount":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 2, limit = 25)

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/mailboxes/mb1/emails", request.url.encodedPath)
        assertEquals("inbox", request.url.parameters["folder"])
        assertEquals("2", request.url.parameters["page"])
        assertEquals("25", request.url.parameters["limit"])
    }

    @Test
    fun `getEmails maps the page dto into the domain EmailPage`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "emails": [
                            {
                                "id": "e1",
                                "subject": "Hello",
                                "sender": "a@example.dev",
                                "recipient": "b@example.dev",
                                "date": "2026-07-16T00:00:00Z",
                                "read": false,
                                "starred": true,
                                "thread_id": "t1",
                                "folder_id": "inbox",
                                "snippet": "preview text"
                            }
                        ],
                        "totalCount": 42
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val page = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50).getOrThrow()

        assertEquals(42, page.totalCount)
        assertEquals(
            EmailSummary(
                id = "e1",
                subject = "Hello",
                sender = "a@example.dev",
                recipient = "b@example.dev",
                date = "2026-07-16T00:00:00Z",
                read = false,
                starred = true,
                threadId = "t1",
                folderId = "inbox",
                snippet = "preview text",
            ),
            page.emails.single(),
        )
    }

    @Test
    fun `getEmails surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertTrue(result.isFailure)
    }
}
