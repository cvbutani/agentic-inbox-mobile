package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
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

class MailboxRepositoryImplTest {

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
    fun `getMailboxes maps every dto field into the domain model`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    [
                        {"id":"mb1","email":"a@example.dev","name":"Alice"},
                        {"id":"mb2","email":"b@example.dev","name":"Bob"}
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getMailboxes()

        assertEquals(
            listOf(
                Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"),
                Mailbox(id = "mb2", email = "b@example.dev", name = "Bob"),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `getMailboxes returns an empty list when the server has none`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getMailboxes()

        assertEquals(emptyList(), result.getOrThrow())
    }

    @Test
    fun `getMailboxes surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getMailboxes()

        assertTrue(result.isFailure)
    }

    @Test
    fun `createMailbox posts the email and name and maps the response`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"mb3","email":"sales@example.dev","name":"Sales"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.createMailbox(email = "sales@example.dev", name = "Sales")

        assertEquals(Mailbox(id = "mb3", email = "sales@example.dev", name = "Sales"), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes", request.url.encodedPath)
    }

    @Test
    fun `createMailbox surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.createMailbox(email = "sales@example.dev", name = "Sales")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getAllowedDomains returns the config's domains`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"domains":["example.dev","example.com"],"emailAddresses":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getAllowedDomains()

        assertEquals(listOf("example.dev", "example.com"), result.getOrThrow())
    }

    @Test
    fun `getAllowedDomains surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getAllowedDomains()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getMailbox fetches and maps the single mailbox`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"mb1","email":"a@example.dev","name":"Alice"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getMailbox("mb1")

        assertEquals(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"), result.getOrThrow())
        assertEquals("/api/v1/mailboxes/mb1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `getMailbox surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.getMailbox("mb1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteMailbox issues a delete request for the mailbox id`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.deleteMailbox("mb1")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1", request.url.encodedPath)
    }

    @Test
    fun `deleteMailbox surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = MailboxRepositoryImpl(apiFor(engine))

        val result = repository.deleteMailbox("mb1")

        assertTrue(result.isFailure)
    }
}
