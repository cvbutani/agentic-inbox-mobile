package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.FeatureFlagsDto
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxDao
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Capability discovery: what Mobile assumes about a Worker depending on how much of `features`
 * the Worker's `/api/v1/config` actually reports. */
class ServerFeaturesTest {

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

    private suspend fun featuresFrom(configJson: String): FeatureFlagsDto {
        val engine = MockEngine { _ ->
            respond(configJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return MailboxRepositoryImpl(apiFor(engine), FakeMailboxDao()).getServerFeatures().getOrThrow()
    }

    @Test
    fun `a legacy worker with no features object gets conservative defaults`() = runTest {
        val features = featuresFrom("""{"domains":["example.dev"],"emailAddresses":[]}""")

        assertFalse(features.aiComposeRewrite)
        assertFalse(features.spamClassification)
        assertFalse(features.autoReply)
        assertFalse(features.agentAccess)
        assertTrue(features.signatures)
    }

    @Test
    fun `flags an older worker omits keep their defaults`() = runTest {
        val features = featuresFrom("""{"domains":[],"emailAddresses":[],"features":{"aiAgent":false,"mcp":true}}""")

        assertFalse(features.aiAgent)
        assertTrue(features.mcp)
        assertFalse(features.aiComposeRewrite)
    }

    @Test
    fun `every flag the current worker reports is parsed`() = runTest {
        val features = featuresFrom(
            """
            {"domains":[],"emailAddresses":[],"features":{
                "aiAgent":true,"mcp":false,"spamClassification":true,"aiComposeRewrite":true,
                "signatures":false,"autoReply":true,"agentAccess":true
            }}
            """.trimIndent(),
        )

        assertTrue(features.spamClassification)
        assertTrue(features.aiComposeRewrite)
        assertFalse(features.signatures)
        assertTrue(features.autoReply)
        assertTrue(features.agentAccess)
    }

    @Test
    fun `AI rewrite is available only when both the agent and the rewrite flag are on`() {
        assertTrue(FeatureFlagsDto(aiAgent = true, aiComposeRewrite = true).aiRewriteAvailable)
        assertFalse(FeatureFlagsDto(aiAgent = false, aiComposeRewrite = true).aiRewriteAvailable)
        assertFalse(FeatureFlagsDto(aiAgent = true, aiComposeRewrite = false).aiRewriteAvailable)
    }
}
