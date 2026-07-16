package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
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

class ConnectionRepositoryImplTest {

    // Mirrors the real client's expectSuccess/followRedirects config in NetworkModule.kt, since
    // describeFailure's error handling depends on both.
    private fun apiFor(engine: MockEngine): AgenticInboxApi {
        val client = HttpClient(engine) {
            expectSuccess = true
            followRedirects = false
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
    fun `validate succeeds when the config endpoint responds 200`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"domains":[],"emailAddresses":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = ConnectionRepositoryImpl(apiFor(engine))

        val result = repository.validate()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate reports Cloudflare Access rejection on a sign-in redirect`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "https://my-team.cloudflareaccess.com/cdn-cgi/access/login"),
            )
        }
        val repository = ConnectionRepositoryImpl(apiFor(engine))

        val result = repository.validate()

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("Cloudflare Access rejected the service token"), "was: $message")
    }

    @Test
    fun `validate surfaces the status code and body for other HTTP errors`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "Internal error detail",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }
        val repository = ConnectionRepositoryImpl(apiFor(engine))

        val result = repository.validate()

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("Server returned HTTP 500"), "was: $message")
        assertTrue(message.contains("Internal error detail"), "was: $message")
    }

    @Test
    fun `validate passes through non-HTTP failures with their own message`() = runTest {
        val engine = MockEngine { _ -> throw IllegalStateException("no network") }
        val repository = ConnectionRepositoryImpl(apiFor(engine))

        val result = repository.validate()

        assertTrue(result.isFailure)
        assertEquals("no network", result.exceptionOrNull()?.message)
    }
}
