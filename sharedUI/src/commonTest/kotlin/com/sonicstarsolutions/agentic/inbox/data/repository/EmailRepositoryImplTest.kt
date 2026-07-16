package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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
            defaultRequest { contentType(ContentType.Application.Json) }
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
    fun `getEmails maps thread_unread_count onto the domain EmailSummary`() = runTest {
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
                                "read": true,
                                "starred": false,
                                "thread_id": "t1",
                                "folder_id": "inbox",
                                "thread_unread_count": 3
                            }
                        ],
                        "totalCount": 1
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val page = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50).getOrThrow()

        assertEquals(3, page.emails.single().threadUnreadCount)
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

    @Test
    fun `moveEmail posts to the move endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.moveEmail(mailboxId = "mb1", emailId = "e1", folderId = "archive")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/move", request.url.encodedPath)
    }

    @Test
    fun `moveEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.moveEmail(mailboxId = "mb1", emailId = "e1", folderId = "archive")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteEmail sends a DELETE for the given email`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.deleteEmail(mailboxId = "mb1", emailId = "e1")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", request.url.encodedPath)
    }

    @Test
    fun `deleteEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.deleteEmail(mailboxId = "mb1", emailId = "e1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `setRead sends a PUT with the requested read state`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","subject":"s","sender":"a","recipient":"b","date":"2026-07-16T00:00:00Z","read":true,"starred":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.setRead(mailboxId = "mb1", emailId = "e1", read = true)

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", request.url.encodedPath)
    }

    @Test
    fun `setRead surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.setRead(mailboxId = "mb1", emailId = "e1", read = false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `markThreadRead posts to the thread read endpoint`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.markThreadRead(mailboxId = "mb1", threadId = "t1")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/threads/t1/read", request.url.encodedPath)
    }

    @Test
    fun `markThreadRead surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.markThreadRead(mailboxId = "mb1", threadId = "t1")

        assertTrue(result.isFailure)
    }

    private fun composeRequest() = ComposeEmailRequest(
        fromEmail = "me@example.dev",
        fromName = "Me",
        to = listOf("alice@example.dev"),
        cc = listOf("bob@example.dev"),
        subject = "Hello",
        body = "Hi there",
    )

    @Test
    fun `sendEmail posts to the emails endpoint`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.sendEmail(mailboxId = "mb1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails", request.url.encodedPath)
    }

    @Test
    fun `sendEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.sendEmail(mailboxId = "mb1", request = composeRequest())

        assertTrue(result.isFailure)
    }

    @Test
    fun `replyEmail posts to the reply endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e2","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.replyEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/reply", request.url.encodedPath)
    }

    @Test
    fun `replyEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.replyEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isFailure)
    }

    @Test
    fun `forwardEmail posts to the forward endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e3","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.forwardEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/forward", request.url.encodedPath)
    }

    @Test
    fun `forwardEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine))

        val result = repository.forwardEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isFailure)
    }
}
