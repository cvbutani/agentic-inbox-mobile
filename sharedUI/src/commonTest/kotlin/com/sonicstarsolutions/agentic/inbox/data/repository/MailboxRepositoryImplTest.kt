package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
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

class MailboxRepositoryImplTest {

    private fun apiFor(engine: MockEngine): AgenticInboxApi {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false })
            }
        }
        return Ktorfit.Builder()
            .baseUrl("https://my-worker.example.dev/", checkUrl = false)
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
}
