package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
import kotlin.test.assertTrue

class ContactRepositoryImplTest {

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
    fun `search asks for the query and limit and maps each contact`() = runTest {
        val engine = MockEngine {
            respond(
                """{"contacts":[{"address":"bob@x.dev","count":7,"last":"2026-11-01T00:00:00Z"},{"address":"bo2@x.dev","count":1,"last":"2026-10-01T00:00:00Z"}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = ContactRepositoryImpl(apiFor(engine)).search("mb1", "bo", limit = 6).getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/mailboxes/mb1/contacts", request.url.encodedPath)
        assertEquals("bo", request.url.parameters["q"])
        assertEquals("6", request.url.parameters["limit"])
        assertEquals(listOf(Contact("bob@x.dev", 7), Contact("bo2@x.dev", 1)), result)
    }

    @Test
    fun `a failure comes back as a failed result`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }

        assertTrue(ContactRepositoryImpl(apiFor(engine)).search("mb1", "bo", 8).isFailure)
    }
}
