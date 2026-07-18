package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.AttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttachmentRepositoryImplTest {

    private class FakeAttachmentStore : AttachmentStore {
        val files = mutableMapOf<String, ByteArray>() // key: "attachmentId/filename"
        val writes = mutableListOf<String>()

        override suspend fun cachedPath(attachmentId: String, filename: String, expectedSize: Long): String? {
            val bytes = files["$attachmentId/$filename"] ?: return null
            return if (bytes.size.toLong() == expectedSize) "/cache/$attachmentId/$filename" else null
        }

        override suspend fun write(attachmentId: String, filename: String, bytes: ByteArray): String {
            files["$attachmentId/$filename"] = bytes
            writes += "$attachmentId/$filename"
            return "/cache/$attachmentId/$filename"
        }
    }

    private fun apiFor(engine: MockEngine): AgenticInboxApi {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false })
            }
        }
        return Ktorfit.Builder()
            .baseUrl("https://my-worker.example.dev", checkUrl = false)
            .httpClient(client)
            .build()
            .createAgenticInboxApi()
    }

    private val attachment = EmailAttachment(
        id = "a1",
        filename = "invoice.pdf",
        mimetype = "application/pdf",
        size = 4,
    )

    @Test
    fun `downloads the bytes, writes them to the store, and returns the cached path`() = runTest {
        val engine = MockEngine { respond(byteArrayOf(1, 2, 3, 4)) }
        val store = FakeAttachmentStore()
        val repository = AttachmentRepositoryImpl(apiFor(engine), store)

        val result = repository.download("mb1", "e1", attachment)

        assertEquals("/cache/a1/invoice.pdf", result.getOrThrow())
        assertEquals(listOf("a1/invoice.pdf"), store.writes)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `a complete cached copy short-circuits the network entirely`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val store = FakeAttachmentStore().apply {
            files["a1/invoice.pdf"] = byteArrayOf(1, 2, 3, 4) // matches attachment.size
        }
        val repository = AttachmentRepositoryImpl(apiFor(engine), store)

        val result = repository.download("mb1", "e1", attachment)

        assertEquals("/cache/a1/invoice.pdf", result.getOrThrow())
        assertEquals(0, engine.requestHistory.size, "a cached attachment must not hit the backend")
    }

    @Test
    fun `an incomplete cached copy is re-downloaded`() = runTest {
        // A partial file from an interrupted download must not be served as the attachment.
        val engine = MockEngine { respond(byteArrayOf(1, 2, 3, 4)) }
        val store = FakeAttachmentStore().apply {
            files["a1/invoice.pdf"] = byteArrayOf(1, 2) // size mismatch
        }
        val repository = AttachmentRepositoryImpl(apiFor(engine), store)

        val result = repository.download("mb1", "e1", attachment)

        assertEquals("/cache/a1/invoice.pdf", result.getOrThrow())
        assertEquals(1, engine.requestHistory.size)
        assertTrue(store.files["a1/invoice.pdf"].contentEquals(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun `a failed download surfaces as a failure`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        val repository = AttachmentRepositoryImpl(apiFor(engine), FakeAttachmentStore())

        val result = repository.download("mb1", "e1", attachment)

        assertTrue(result.isFailure)
    }
}
