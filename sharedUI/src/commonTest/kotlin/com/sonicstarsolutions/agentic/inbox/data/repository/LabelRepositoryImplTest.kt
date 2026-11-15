package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabelRepositoryImplTest {

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

    private fun engineReplying(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { respond(body, status, json) }

    @Test
    fun `getLabels unwraps the labels array and maps each label`() = runTest {
        val engine = engineReplying(
            """{"labels":[{"id":"work","name":"Work","color":"blue","count":4},{"id":"fun","name":"Fun","color":"red","count":0}]}""",
        )

        val result = LabelRepositoryImpl(apiFor(engine)).getLabels("mb1")

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/mailboxes/mb1/labels", request.url.encodedPath)
        assertEquals(
            listOf(Label("work", "Work", "blue", count = 4), Label("fun", "Fun", "red", count = 0)),
            result.getOrThrow(),
        )
    }

    @Test
    fun `createLabel posts the name and colour and returns the new label`() = runTest {
        val engine = engineReplying("""{"id":"client-work","name":"Client work","color":"teal"}""", HttpStatusCode.Created)

        val result = LabelRepositoryImpl(apiFor(engine)).createLabel("mb1", "Client work", "teal")

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/labels", request.url.encodedPath)
        val body = (request.body as TextContent).text
        assertTrue(body.contains("\"name\":\"Client work\"") && body.contains("\"color\":\"teal\""), body)
        assertEquals(Label("client-work", "Client work", "teal"), result.getOrThrow())
    }

    @Test
    fun `updateLabel sends only the changed fields`() = runTest {
        val engine = engineReplying("""{"id":"work","name":"Work","color":"purple"}""")

        val result = LabelRepositoryImpl(apiFor(engine)).updateLabel("mb1", "work", name = null, color = "purple")

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/mailboxes/mb1/labels/work", request.url.encodedPath)
        val body = (request.body as TextContent).text
        assertTrue(body.contains("\"color\":\"purple\"") && !body.contains("name"), body)
        assertEquals("purple", result.getOrThrow().color)
    }

    @Test
    fun `deleteLabel sends a DELETE and treats 204 as success`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }

        val result = LabelRepositoryImpl(apiFor(engine)).deleteLabel("mb1", "work")

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/labels/work", request.url.encodedPath)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `a duplicate name comes back as a presentable conflict`() = runTest {
        val engine = engineReplying("""{"error":"A label with this name already exists"}""", HttpStatusCode.Conflict)

        val failure = LabelRepositoryImpl(apiFor(engine)).createLabel("mb1", "Work", "blue").exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals("A label with this name already exists", failure.message)
    }
}
