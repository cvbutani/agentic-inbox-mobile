package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.local.FolderEntity
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderDao
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
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FolderRepositoryImplTest {

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
    fun `getFolders merges server unread counts onto the system folder defaults`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    [
                        {"id":"inbox","name":"Inbox","unreadCount":7},
                        {"id":"draft","name":"Drafts","unreadCount":0},
                        {"id":"sent","name":"Sent","unreadCount":0},
                        {"id":"archive","name":"Archive","unreadCount":0},
                        {"id":"spam","name":"Spam","unreadCount":3},
                        {"id":"trash","name":"Trash","unreadCount":0}
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val folders = repository.getFolders("mb1").getOrThrow()

        assertEquals(
            listOf(
                Folder(id = "inbox", name = "Inbox", unreadCount = 7, isSystem = true),
                Folder(id = "draft", name = "Drafts", unreadCount = 0, isSystem = true),
                Folder(id = "sent", name = "Sent", unreadCount = 0, isSystem = true),
                Folder(id = "archive", name = "Archive", unreadCount = 0, isSystem = true),
                Folder(id = "spam", name = "Junk", unreadCount = 3, isSystem = true),
                Folder(id = "trash", name = "Trash", unreadCount = 0, isSystem = true),
            ),
            folders,
        )
    }

    @Test
    fun `getFolders keeps system defaults with zero unread when the server omits them`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """[{"id":"work","name":"Work","unreadCount":2}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val folders = repository.getFolders("mb1").getOrThrow()

        assertEquals(SystemFolders.defaults, folders.take(6))
        assertEquals(Folder(id = "work", name = "Work", unreadCount = 2, isSystem = false), folders.last())
    }

    @Test
    fun `getFolders appends custom folders after the system folders`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    [
                        {"id":"inbox","name":"Inbox","unreadCount":1},
                        {"id":"work","name":"Work","unreadCount":4},
                        {"id":"receipts","name":"Receipts","unreadCount":0}
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val folders = repository.getFolders("mb1").getOrThrow()

        val customFolders = folders.filterNot { it.isSystem }
        assertEquals(
            listOf(
                Folder(id = "work", name = "Work", unreadCount = 4, isSystem = false),
                Folder(id = "receipts", name = "Receipts", unreadCount = 0, isSystem = false),
            ),
            customFolders,
        )
    }

    @Test
    fun `getFolders surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.getFolders("mb1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `createFolder posts the name and maps the response into a custom folder`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"work","name":"Work","unreadCount":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.createFolder(mailboxId = "mb1", name = "Work")

        assertEquals(Folder(id = "work", name = "Work", unreadCount = 0, isSystem = false), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/folders", request.url.encodedPath)
    }

    @Test
    fun `createFolder surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.createFolder(mailboxId = "mb1", name = "Work")

        assertTrue(result.isFailure)
    }

    @Test
    fun `renameFolder puts the new name and maps the response into a custom folder`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"work","name":"Projects","unreadCount":4}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.renameFolder(mailboxId = "mb1", folderId = "work", name = "Projects")

        assertEquals(Folder(id = "work", name = "Projects", unreadCount = 4, isSystem = false), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/mailboxes/mb1/folders/work", request.url.encodedPath)
    }

    @Test
    fun `renameFolder surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.renameFolder(mailboxId = "mb1", folderId = "work", name = "Projects")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteFolder issues a delete request for the folder id`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.deleteFolder(mailboxId = "mb1", folderId = "work")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/folders/work", request.url.encodedPath)
    }

    @Test
    fun `deleteFolder surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.deleteFolder(mailboxId = "mb1", folderId = "work")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getFolders caches the resolved list on success`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """[{"id":"inbox","name":"Inbox","unreadCount":2},{"id":"work","name":"Work","unreadCount":1}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dao = FakeFolderDao()
        val repository = FolderRepositoryImpl(apiFor(engine), dao)

        repository.getFolders("mb1")

        val cached = dao.getForMailbox("mb1")
        assertTrue(cached.any { it.id == "inbox" && it.unreadCount == 2 })
        assertTrue(cached.any { it.id == "work" && it.unreadCount == 1 })
    }

    @Test
    fun `getFolders replaces stale cached rows for the mailbox on success`() = runTest {
        val dao = FakeFolderDao()
        dao.upsertAll(listOf(FolderEntity(mailboxId = "mb1", id = "stale", name = "Stale", unreadCount = 0, isSystem = false)))
        val engine = MockEngine { _ ->
            respond(
                content = """[{"id":"inbox","name":"Inbox","unreadCount":0}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = FolderRepositoryImpl(apiFor(engine), dao)

        repository.getFolders("mb1")

        assertTrue(dao.getForMailbox("mb1").none { it.id == "stale" })
    }

    @Test
    fun `getFolders falls back to the cache when the network call fails`() = runTest {
        val dao = FakeFolderDao()
        dao.upsertAll(listOf(FolderEntity(mailboxId = "mb1", id = "work", name = "Work", unreadCount = 3, isSystem = false)))
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), dao)

        val result = repository.getFolders("mb1")

        assertEquals(listOf(Folder(id = "work", name = "Work", unreadCount = 3, isSystem = false)), result.getOrThrow())
    }

    @Test
    fun `getFolders surfaces the original failure when the network fails and the cache is empty`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), FakeFolderDao())

        val result = repository.getFolders("mb1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `clearCache empties the cached folder table for every mailbox`() = runTest {
        val dao = FakeFolderDao()
        dao.upsertAll(
            listOf(
                FolderEntity(mailboxId = "mb1", id = "work", name = "Work", unreadCount = 0, isSystem = false),
                FolderEntity(mailboxId = "mb2", id = "receipts", name = "Receipts", unreadCount = 0, isSystem = false),
            ),
        )
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = FolderRepositoryImpl(apiFor(engine), dao)

        repository.clearCache()

        assertTrue(dao.getForMailbox("mb1").isEmpty())
        assertTrue(dao.getForMailbox("mb2").isEmpty())
    }
}
