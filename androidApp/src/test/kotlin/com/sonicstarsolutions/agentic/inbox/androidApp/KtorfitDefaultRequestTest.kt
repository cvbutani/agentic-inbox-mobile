package com.sonicstarsolutions.agentic.inbox.androidApp

import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JVM-executable mirror of sharedUI's commonTest KtorfitDefaultRequestTest (the KMP module has no
 * host-side test runner on this AGP9 setup, so this copy actually runs in CI / on dev machines).
 *
 * Proves end-to-end — real Ktorfit-generated client, real DefaultRequest plugin, MockEngine —
 * that NetworkModule's setup routes requests to the credentials-supplied URL and not localhost.
 */
class KtorfitDefaultRequestTest {

    private fun buildClient(engine: MockEngine, credentials: Credentials): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false })
            }
            defaultRequest {
                val base = credentials.normalizedBaseUrl()
                if (base.isNotEmpty()) {
                    url.takeFrom(base)
                }
                if (credentials.clientId.isNotBlank()) header("CF-Access-Client-Id", credentials.clientId)
                if (credentials.clientSecret.isNotBlank()) header("CF-Access-Client-Secret", credentials.clientSecret)
            }
        }

    private fun mockEngine(): MockEngine =
        MockEngine { _ ->
            respond(
                content = """{"domains":[],"emailAddresses":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

    @Test
    fun `ktorfit call goes to the credentials url instead of localhost`() = runTest {
        val engine = mockEngine()
        val credentials = Credentials(
            baseUrl = "https://my-worker.example.dev",
            clientId = "id",
            clientSecret = "secret",
        )

        val api = Ktorfit.Builder()
            .baseUrl("", checkUrl = false)
            .httpClient(buildClient(engine, credentials))
            .build()
            .createAgenticInboxApi()

        api.getConfig()

        val request = engine.requestHistory.single()
        assertEquals("https://my-worker.example.dev/api/v1/config", request.url.toString())
        assertEquals("id", request.headers["CF-Access-Client-Id"])
        assertEquals("secret", request.headers["CF-Access-Client-Secret"])
    }

    @Test
    fun `scheme-less saved url is normalized and still reaches the worker`() = runTest {
        val engine = mockEngine()
        // What a user actually types into the onboarding field — no scheme.
        val credentials = Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "secret")

        val api = Ktorfit.Builder()
            .baseUrl("", checkUrl = false)
            .httpClient(buildClient(engine, credentials))
            .build()
            .createAgenticInboxApi()

        api.getConfig()

        assertEquals(
            "https://my-worker.example.dev/api/v1/config",
            engine.requestHistory.single().url.toString(),
        )
    }
}
