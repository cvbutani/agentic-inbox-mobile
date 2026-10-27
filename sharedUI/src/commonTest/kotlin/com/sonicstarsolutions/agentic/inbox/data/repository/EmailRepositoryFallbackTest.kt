package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.FeatureUnavailableException
import com.sonicstarsolutions.agentic.inbox.domain.model.MarkSpamOutcome
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailDao
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

/** Optional Worker endpoints (`/spam`, `/ai/compose-rewrite`): an older or AI-less Worker answers
 * 404/501 and the client must degrade, while genuine errors must stay visible. */
class EmailRepositoryFallbackTest {

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

    @Test
    fun `markSpam reports MARKED with a single request when the worker supports spam`() = runTest {
        val engine = MockEngine { respond("""{"success":true,"emailId":"e1","isSpam":true,"folderId":"spam"}""", HttpStatusCode.OK, json) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.markSpam("mb1", "e1", spam = true)

        assertEquals(MarkSpamOutcome.MARKED, result.getOrThrow())
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/spam", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `markSpam moves to Trash and reports the fallback when the spam endpoint is 404`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/spam")) respond("", HttpStatusCode.NotFound)
            else respond("""{"status":"moved"}""", HttpStatusCode.OK, json)
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.markSpam("mb1", "e1", spam = true)

        assertEquals(MarkSpamOutcome.FALLBACK_MOVED, result.getOrThrow())
        assertEquals(2, engine.requestHistory.size)
        assertTrue(engine.requestHistory.last().url.encodedPath.endsWith("/move"))
    }

    @Test
    fun `markSpam falls back on 501 too`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/spam")) respond("", HttpStatusCode.NotImplemented)
            else respond("""{"status":"moved"}""", HttpStatusCode.OK, json)
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        assertEquals(MarkSpamOutcome.FALLBACK_MOVED, repository.markSpam("mb1", "e1", spam = false).getOrThrow())
    }

    @Test
    fun `markSpam does not silently move the email when the failure is not a missing endpoint`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.markSpam("mb1", "e1", spam = true)

        assertTrue(result.isFailure)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `markSpam fails when the fallback move fails too`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NotFound) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        assertTrue(repository.markSpam("mb1", "e1", spam = true).isFailure)
    }

    @Test
    fun `rewriteDraft returns the rewritten text`() = runTest {
        val engine = MockEngine { respond("""{"rewritten":"Polished"}""", HttpStatusCode.OK, json) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        assertEquals("Polished", repository.rewriteDraft("mb1", "rough", "fix").getOrThrow())
    }

    @Test
    fun `rewriteDraft maps 404 and 501 to FeatureUnavailableException`() = runTest {
        for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.NotImplemented)) {
            val engine = MockEngine { respond("", status) }
            val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

            val failure = repository.rewriteDraft("mb1", "rough", "fix").exceptionOrNull()

            assertTrue(failure is FeatureUnavailableException, "status $status gave $failure")
        }
    }

    @Test
    fun `rewriteDraft leaves other failures as ordinary errors`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val failure = repository.rewriteDraft("mb1", "rough", "fix").exceptionOrNull()

        assertTrue(failure != null && failure !is FeatureUnavailableException)
    }
}
