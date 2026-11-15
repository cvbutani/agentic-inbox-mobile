package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailDao
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Worker answers 400 with `{error, details}` (Zod) or `{error}`, and 429 with `{error}`.
 * The repository layer turns those into one presentable [ApiException] instead of leaking Ktor's
 * verbose "Client request(...) invalid: 400 Bad Request. Text: ..." message into the UI. */
class ApiErrorMappingTest {

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

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private suspend fun failureFor(status: HttpStatusCode, body: String, headers: io.ktor.http.Headers = json): Throwable {
        val engine = MockEngine { respond(body, status, headers) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        return repository.moveEmail("mb1", "e1", "inbox").exceptionOrNull()!!
    }

    @Test
    fun `a 400 with Zod field errors becomes an ApiException naming each field`() = runTest {
        val body = """
            {"error":"Invalid request body","details":{"formErrors":[],"fieldErrors":{"folderId":["Required"],"to":["Invalid email","Too long"]}}}
        """.trimIndent()

        val failure = failureFor(HttpStatusCode.BadRequest, body)

        assertTrue(failure is ApiException)
        assertEquals(400, failure.status)
        assertEquals(listOf("Required"), failure.fieldErrors["folderId"])
        assertEquals(listOf("Invalid email", "Too long"), failure.fieldErrors["to"])
        assertEquals("Invalid request body — folderId: Required; to: Invalid email, Too long", failure.message)
    }

    @Test
    fun `a 400 with form-level errors includes them in the message`() = runTest {
        val body = """{"error":"Invalid request body","details":{"formErrors":["Scheduled messages cannot have attachments yet"],"fieldErrors":{}}}"""

        val failure = failureFor(HttpStatusCode.BadRequest, body)

        assertTrue(failure is ApiException)
        assertEquals("Invalid request body — Scheduled messages cannot have attachments yet", failure.message)
    }

    @Test
    fun `a 400 with only an error string uses that string as the message`() = runTest {
        val failure = failureFor(HttpStatusCode.BadRequest, """{"error":"Folder not found"}""")

        assertTrue(failure is ApiException)
        assertEquals("Folder not found", failure.message)
        assertTrue(failure.fieldErrors.isEmpty())
    }

    @Test
    fun `a 400 whose body is not the worker's JSON still gets a clean message`() = runTest {
        for (body in listOf("", "<html>Bad Request</html>", "[1,2]")) {
            val failure = failureFor(HttpStatusCode.BadRequest, body)

            assertTrue(failure is ApiException, "body '$body' gave $failure")
            assertEquals("The server rejected the request (400)", failure.message)
        }
    }

    @Test
    fun `a 429 carries the worker's message and the Retry-After delay`() = runTest {
        val headers = headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.RetryAfter to listOf("30"),
        )

        val failure = failureFor(
            HttpStatusCode.TooManyRequests,
            """{"error":"Rate limit exceeded: max 20 emails per hour per mailbox"}""",
            headers,
        )

        assertTrue(failure is ApiException)
        assertEquals(429, failure.status)
        assertEquals("Rate limit exceeded: max 20 emails per hour per mailbox", failure.message)
        assertEquals(30, failure.retryAfterSeconds)
    }

    @Test
    fun `a bare 429 explains itself and mentions the wait when Retry-After is present`() = runTest {
        val plain = failureFor(HttpStatusCode.TooManyRequests, "")
        assertTrue(plain is ApiException)
        assertEquals("Too many requests — please try again shortly", plain.message)
        assertNull(plain.retryAfterSeconds)

        val withWait = failureFor(
            HttpStatusCode.TooManyRequests,
            "",
            headersOf(HttpHeaders.RetryAfter, "45"),
        )
        assertTrue(withWait is ApiException)
        assertEquals("Too many requests — please try again in 45 seconds", withWait.message)
    }

    @Test
    fun `an unparseable Retry-After is ignored`() = runTest {
        val failure = failureFor(
            HttpStatusCode.TooManyRequests,
            "",
            headersOf(HttpHeaders.RetryAfter, "Wed, 21 Oct 2026 07:28:00 GMT"),
        )

        assertTrue(failure is ApiException)
        assertNull(failure.retryAfterSeconds)
        assertEquals("Too many requests — please try again shortly", failure.message)
    }

    @Test
    fun `a 502 from a rejected send carries the provider's message`() = runTest {
        val failure = failureFor(
            HttpStatusCode.BadGateway,
            """{"error":"Failed to send email: email to nobody@example.org not allowed"}""",
        )

        assertTrue(failure is ApiException)
        assertEquals(502, failure.status)
        assertEquals("Failed to send email: email to nobody@example.org not allowed", failure.message)
    }

    @Test
    fun `a 502 without the worker's JSON still gets a clean message`() = runTest {
        val failure = failureFor(HttpStatusCode.BadGateway, "<html>Bad Gateway</html>")

        assertTrue(failure is ApiException)
        assertEquals("The server couldn't complete the request (502)", failure.message)
    }

    @Test
    fun `a 404 with the server's JSON error is presentable`() = runTest {
        val failure = failureFor(HttpStatusCode.NotFound, """{"error":"Folder not found"}""")

        assertTrue(failure is ApiException)
        assertEquals(404, failure.status)
        assertEquals("Folder not found", failure.message)
    }

    @Test
    fun `a 409 conflict carries the server's message`() = runTest {
        val failure = failureFor(HttpStatusCode.Conflict, """{"error":"Mailbox already exists"}""")

        assertTrue(failure is ApiException)
        assertEquals(409, failure.status)
        assertEquals("Mailbox already exists", failure.message)
    }

    @Test
    fun `a 403 with the server's JSON error is presentable but an HTML 403 is left raw`() = runTest {
        val restricted = failureFor(
            HttpStatusCode.Forbidden,
            """{"error":"Mailbox creation is restricted to configured EMAIL_ADDRESSES"}""",
        )
        assertTrue(restricted is ApiException)
        assertEquals(403, restricted.status)
        assertEquals("Mailbox creation is restricted to configured EMAIL_ADDRESSES", restricted.message)

        // A Cloudflare Access denial is HTML; its details must stay visible to the connection check.
        val access = failureFor(HttpStatusCode.Forbidden, "<html>Access denied</html>")
        assertTrue(access is ResponseException)
    }

    @Test
    fun `other statuses are left as raw Ktor exceptions`() = runTest {
        val serverError = failureFor(HttpStatusCode.InternalServerError, """{"error":"boom"}""")
        val notFound = failureFor(HttpStatusCode.NotFound, "<html>Not Found</html>")

        assertTrue(serverError is ResponseException)
        assertTrue(notFound is ResponseException)
    }

    @Test
    fun `network failures pass through untouched`() = runTest {
        val engine = MockEngine { throw IllegalStateException("offline") }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val failure = repository.moveEmail("mb1", "e1", "inbox").exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }
}
