package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxDao
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailboxSettingsRepositoryTest {

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

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val serverSettings = """
        {"fromName":"Alice","forwarding":{"enabled":true,"email":"fwd@example.dev"},"autoDraft":{"enabled":false},
         "signature":{"enabled":false,"text":"","html":""},
         "autoReply":{"enabled":false,"subject":"","message":""}}
    """.trimIndent()

    private fun mailboxJson(settings: String) = """{"id":"mb1","email":"mb1@example.dev","name":"mb1","settings":$settings}"""

    private fun HttpRequestData.bodyJson() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun `getMailboxSettings reads and parses the mailbox's settings`() = runTest {
        val engine = MockEngine { respond(mailboxJson(serverSettings), HttpStatusCode.OK, jsonHeaders) }
        val repository = MailboxRepositoryImpl(apiFor(engine), FakeMailboxDao())

        val settings = repository.getMailboxSettings("mb1").getOrThrow()

        assertEquals("Alice", settings.fromName)
        assertEquals("/api/v1/mailboxes/mb1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `updateMailboxSettings reads first, then PUTs the merge so unedited settings survive`() = runTest {
        val engine = MockEngine { request ->
            val settings = if (request.method == HttpMethod.Put) request.bodyJson()["settings"].toString() else serverSettings
            respond(mailboxJson(settings), HttpStatusCode.OK, jsonHeaders)
        }
        val repository = MailboxRepositoryImpl(apiFor(engine), FakeMailboxDao())

        val saved = repository.updateMailboxSettings(
            "mb1",
            MailboxSettings(fromName = "Alicia", signatureEnabled = true, signatureText = "Cheers"),
        ).getOrThrow()

        assertEquals(listOf(HttpMethod.Get, HttpMethod.Put), engine.requestHistory.map { it.method })
        val sent = engine.requestHistory.last().bodyJson()["settings"]!!.jsonObject
        assertEquals("Alicia", sent["fromName"]!!.jsonPrimitive.content)
        assertEquals("fwd@example.dev", sent["forwarding"]!!.jsonObject["email"]!!.jsonPrimitive.content)
        assertEquals("false", sent["autoDraft"]!!.jsonObject["enabled"]!!.jsonPrimitive.content)
        assertEquals("Alicia", saved.fromName)
        assertTrue(saved.signatureEnabled)
        assertEquals("Cheers", saved.signatureText)
    }

    @Test
    fun `a failed read stops before any write`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val repository = MailboxRepositoryImpl(apiFor(engine), FakeMailboxDao())

        val result = repository.updateMailboxSettings("mb1", MailboxSettings(fromName = "X"))

        assertTrue(result.isFailure)
        assertEquals(listOf(HttpMethod.Get), engine.requestHistory.map { it.method })
    }

    @Test
    fun `a rejected write surfaces the worker's message`() = runTest {
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Put) respond("""{"error":"Invalid request body"}""", HttpStatusCode.BadRequest, jsonHeaders)
            else respond(mailboxJson(serverSettings), HttpStatusCode.OK, jsonHeaders)
        }
        val repository = MailboxRepositoryImpl(apiFor(engine), FakeMailboxDao())

        val failure = repository.updateMailboxSettings("mb1", MailboxSettings(fromName = "X")).exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals("Invalid request body", failure.message)
    }

    @Test
    fun `a successful save refreshes the cached display name`() = runTest {
        val dao = FakeMailboxDao()
        val engine = MockEngine { request ->
            val settings = if (request.method == HttpMethod.Put) request.bodyJson()["settings"].toString() else serverSettings
            respond(mailboxJson(settings), HttpStatusCode.OK, jsonHeaders)
        }
        val repository = MailboxRepositoryImpl(apiFor(engine), dao)

        repository.updateMailboxSettings("mb1", MailboxSettings(fromName = "Alicia")).getOrThrow()

        assertEquals("Alicia", dao.getAll().single { it.id == "mb1" }.fromName)
    }
}
