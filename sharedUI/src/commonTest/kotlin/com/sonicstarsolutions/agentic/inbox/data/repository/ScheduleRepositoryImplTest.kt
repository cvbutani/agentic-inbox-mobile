package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledStatus
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
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ScheduleRepositoryImplTest {

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
    private val sendAt = Instant.parse("2026-11-19T08:00:00Z")

    private fun request() = ComposeEmailRequest(
        fromEmail = "me@example.dev",
        fromName = "Me",
        to = listOf("alice@example.dev"),
        cc = listOf("bob@example.dev"),
        subject = "Hello",
        body = "Hi there",
    )

    private fun bodyJson(engine: MockEngine) = Json.parseToJsonElement((engine.requestHistory.single().body as TextContent).text).jsonObject

    @Test
    fun `scheduling a new message posts a compose request with the send time and the whole email`() = runTest {
        val engine = MockEngine { respond("""{"id":"s1","status":"scheduled","send_at":"2026-11-19T08:00:00.000Z"}""", HttpStatusCode.Created, json) }

        val result = ScheduleRepositoryImpl(apiFor(engine)).schedule("mb1", request(), ScheduleKind.COMPOSE, originalEmailId = null, sendAt = sendAt)

        assertTrue(result.isSuccess)
        val sent = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/schedule", sent.url.encodedPath)
        val body = bodyJson(engine)
        assertEquals("compose", body["kind"]!!.jsonPrimitive.content)
        assertEquals("2026-11-19T08:00:00Z", body["send_at"]!!.jsonPrimitive.content)
        assertTrue("originalEmailId" !in body, body.toString())
        val email = body["email"]!!.jsonObject
        assertEquals("Hello", email["subject"]!!.jsonPrimitive.content)
        assertEquals("Hi there", email["text"]!!.jsonPrimitive.content)
        assertEquals(listOf("alice@example.dev"), email["to"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("me@example.dev", email["from"]!!.jsonObject["email"]!!.jsonPrimitive.content)
    }

    @Test
    fun `scheduling a reply names the message being replied to`() = runTest {
        val engine = MockEngine { respond("""{"id":"s1","status":"scheduled","send_at":"2026-11-19T08:00:00.000Z"}""", HttpStatusCode.Created, json) }

        ScheduleRepositoryImpl(apiFor(engine)).schedule("mb1", request(), ScheduleKind.REPLY, originalEmailId = "e7", sendAt = sendAt)

        val body = bodyJson(engine)
        assertEquals("reply", body["kind"]!!.jsonPrimitive.content)
        assertEquals("e7", body["originalEmailId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a refused time comes back as the server's message`() = runTest {
        val engine = MockEngine { respond("""{"error":"send_at must be at least a minute in the future"}""", HttpStatusCode.BadRequest, json) }

        val failure = ScheduleRepositoryImpl(apiFor(engine))
            .schedule("mb1", request(), ScheduleKind.COMPOSE, null, sendAt).exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals("send_at must be at least a minute in the future", failure.message)
    }

    @Test
    fun `getScheduled maps each queued message and its status`() = runTest {
        val engine = MockEngine {
            respond(
                """{"scheduled":[
                    {"id":"a","kind":"compose","send_at":"2026-11-19T08:00:00.000Z","status":"pending","error":null,"to":"alice@example.dev","subject":"Hi"},
                    {"id":"b","kind":"reply","send_at":"2026-11-20T08:00:00.000Z","status":"failed","error":"Rate limit exceeded","to":"bob@example.dev, cy@example.dev","subject":"Re: Plans"},
                    {"id":"c","kind":"compose","send_at":"2026-11-21T08:00:00.000Z","status":"sending","error":null,"to":"d@example.dev","subject":""},
                    {"id":"d","kind":"compose","send_at":"2026-11-22T08:00:00.000Z","status":"something-new","to":"e@example.dev","subject":"S"}
                ]}""",
                HttpStatusCode.OK,
                json,
            )
        }

        val result = ScheduleRepositoryImpl(apiFor(engine)).getScheduled("mb1").getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/mailboxes/mb1/scheduled", request.url.encodedPath)
        assertEquals(
            listOf(
                ScheduledEmail("a", ScheduleKind.COMPOSE, "2026-11-19T08:00:00.000Z", ScheduledStatus.PENDING, null, "alice@example.dev", "Hi"),
                ScheduledEmail("b", ScheduleKind.REPLY, "2026-11-20T08:00:00.000Z", ScheduledStatus.FAILED, "Rate limit exceeded", "bob@example.dev, cy@example.dev", "Re: Plans"),
                ScheduledEmail("c", ScheduleKind.COMPOSE, "2026-11-21T08:00:00.000Z", ScheduledStatus.SENDING, null, "d@example.dev", ""),
                // A status this app doesn't know yet is shown as waiting rather than dropped.
                ScheduledEmail("d", ScheduleKind.COMPOSE, "2026-11-22T08:00:00.000Z", ScheduledStatus.PENDING, null, "e@example.dev", "S"),
            ),
            result,
        )
    }

    @Test
    fun `cancel sends a DELETE and treats 204 as success`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }

        val result = ScheduleRepositoryImpl(apiFor(engine)).cancel("mb1", "s1")

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/scheduled/s1", request.url.encodedPath)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelling a message that is already sending reports why`() = runTest {
        val engine = MockEngine { respond("""{"error":"Not found, or already sending"}""", HttpStatusCode.NotFound, json) }

        val failure = ScheduleRepositoryImpl(apiFor(engine)).cancel("mb1", "s1").exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals(404, failure.status)
        assertEquals("Not found, or already sending", failure.message)
        assertNull(failure.cause)
    }
}
