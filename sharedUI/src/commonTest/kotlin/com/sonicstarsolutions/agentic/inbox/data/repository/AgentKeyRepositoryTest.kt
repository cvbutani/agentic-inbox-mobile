package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.domain.model.FeatureUnavailableException
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentKeyRepositoryTest {

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

    private fun HttpRequestData.bodyJson() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    private val keyJson = """
        {"id":"0123456789abcdef","prefix":"agt_0123456789abcdef","name":"Claude Code","mailboxId":"mb1",
         "scopes":["read","draft"],"dailySendLimit":null,"createdAt":"2026-10-27T09:00:00.000Z","revokedAt":null}
    """.trimIndent()

    @Test
    fun `listKeys reads the mailbox's keys and maps them`() = runTest {
        val engine = MockEngine {
            respond(
                """[$keyJson,{"id":"fedcba9876543210","prefix":"agt_fedcba9876543210","name":"Digest","mailboxId":"mb1",
                    "scopes":["read","send","teleport"],"dailySendLimit":5,"createdAt":"2026-10-26T09:00:00.000Z",
                    "revokedAt":"2026-10-27T08:00:00.000Z"}]""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        val keys = repository.listKeys("mb1").getOrThrow()

        assertEquals("/api/v1/mailboxes/mb1/agent-keys", engine.requestHistory.single().url.encodedPath)
        assertEquals(2, keys.size)
        assertEquals("Claude Code", keys[0].name)
        assertEquals(listOf(AgentScope.READ, AgentScope.DRAFT), keys[0].scopes)
        assertNull(keys[0].dailySendLimit)
        assertFalse(keys[0].revoked)
        // A scope this app doesn't know (from a newer Worker) is skipped rather than failing the list.
        assertEquals(listOf(AgentScope.READ, AgentScope.SEND), keys[1].scopes)
        assertEquals(5, keys[1].dailySendLimit)
        assertTrue(keys[1].revoked)
    }

    @Test
    fun `createKey posts the name and scopes, and returns the one-time secret`() = runTest {
        val engine = MockEngine {
            respond("""{"key":$keyJson,"secret":"agt_0123456789abcdef_SECRET"}""", HttpStatusCode.Created, jsonHeaders)
        }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        val created = repository.createKey("mb1", NewAgentKey("Claude Code", listOf(AgentScope.READ, AgentScope.DRAFT), null)).getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/agent-keys", request.url.encodedPath)
        val body = request.bodyJson()
        assertEquals("Claude Code", body["name"]!!.jsonPrimitive.content)
        assertEquals(listOf("read", "draft"), body["scopes"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertFalse("dailySendLimit" in body, "an absent limit must be omitted, not sent as null")
        assertEquals("agt_0123456789abcdef_SECRET", created.secret)
        assertEquals("Claude Code", created.key.name)
    }

    @Test
    fun `createKey sends the daily send limit when one is set`() = runTest {
        val engine = MockEngine { respond("""{"key":$keyJson,"secret":"s"}""", HttpStatusCode.Created, jsonHeaders) }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        repository.createKey("mb1", NewAgentKey("Sender", listOf(AgentScope.SEND), 12)).getOrThrow()

        assertEquals(12, engine.requestHistory.single().bodyJson()["dailySendLimit"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `revokeKey deletes the key`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        repository.revokeKey("mb1", "0123456789abcdef").getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/agent-keys/0123456789abcdef", request.url.encodedPath)
    }

    @Test
    fun `a worker without agent access answers 404 and that becomes FeatureUnavailable`() = runTest {
        val engine = MockEngine { respond("""{"error":"Agent access is not enabled on this worker"}""", HttpStatusCode.NotFound, jsonHeaders) }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        assertTrue(repository.listKeys("mb1").exceptionOrNull() is FeatureUnavailableException)
        assertTrue(repository.createKey("mb1", NewAgentKey("k", listOf(AgentScope.READ), null)).exceptionOrNull() is FeatureUnavailableException)
    }

    @Test
    fun `other failures stay ordinary errors`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val repository = AgentKeyRepositoryImpl(apiFor(engine))

        val failure = repository.listKeys("mb1").exceptionOrNull()

        assertTrue(failure != null && failure !is FeatureUnavailableException)
    }
}
