package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.BulkAction
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.data.local.EmailEntity
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailDao
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailRepositoryImplTest {

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
    fun `getEmails requests the given mailbox folder and page`() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """{"emails":[],"totalCount":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 2, limit = 25)

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/mailboxes/mb1/emails", request.url.encodedPath)
        assertEquals("inbox", request.url.parameters["folder"])
        assertEquals("2", request.url.parameters["page"])
        assertEquals("25", request.url.parameters["limit"])
    }

    @Test
    fun `getEmails maps the page dto into the domain EmailPage`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "emails": [
                            {
                                "id": "e1",
                                "subject": "Hello",
                                "sender": "a@example.dev",
                                "recipient": "b@example.dev",
                                "date": "2026-07-16T00:00:00Z",
                                "read": false,
                                "starred": true,
                                "thread_id": "t1",
                                "folder_id": "inbox",
                                "snippet": "preview text"
                            }
                        ],
                        "totalCount": 42
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50).getOrThrow()

        assertEquals(42, page.totalCount)
        assertEquals(
            EmailSummary(
                id = "e1",
                subject = "Hello",
                sender = "a@example.dev",
                recipient = "b@example.dev",
                date = "2026-07-16T00:00:00Z",
                read = false,
                starred = true,
                threadId = "t1",
                folderId = "inbox",
                snippet = "preview text",
                mailboxId = "mb1",
            ),
            page.emails.single(),
        )
    }

    @Test
    fun `getEmails maps thread_unread_count onto the domain EmailSummary`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "emails": [
                            {
                                "id": "e1",
                                "subject": "Hello",
                                "sender": "a@example.dev",
                                "recipient": "b@example.dev",
                                "date": "2026-07-16T00:00:00Z",
                                "read": true,
                                "starred": false,
                                "thread_id": "t1",
                                "folder_id": "inbox",
                                "thread_unread_count": 3
                            }
                        ],
                        "totalCount": 1
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50).getOrThrow()

        assertEquals(3, page.emails.single().threadUnreadCount)
    }

    @Test
    fun `getEmails surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertTrue(result.isFailure)
    }

    @Test
    fun `moveEmail posts to the move endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.moveEmail(mailboxId = "mb1", emailId = "e1", folderId = "archive")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/move", request.url.encodedPath)
    }

    @Test
    fun `moveEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.moveEmail(mailboxId = "mb1", emailId = "e1", folderId = "archive")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteEmail sends a DELETE for the given email`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.deleteEmail(mailboxId = "mb1", emailId = "e1")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", request.url.encodedPath)
    }

    @Test
    fun `deleteEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.deleteEmail(mailboxId = "mb1", emailId = "e1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `setRead sends a PUT with the requested read state`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","subject":"s","sender":"a","recipient":"b","date":"2026-07-16T00:00:00Z","read":true,"starred":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.setRead(mailboxId = "mb1", emailId = "e1", read = true)

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", request.url.encodedPath)
    }

    @Test
    fun `setRead surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.setRead(mailboxId = "mb1", emailId = "e1", read = false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `setStarred sends a PUT with the requested starred state`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","subject":"s","sender":"a","recipient":"b","date":"2026-07-16T00:00:00Z","read":true,"starred":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.setStarred(mailboxId = "mb1", emailId = "e1", starred = true)

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1", request.url.encodedPath)
    }

    @Test
    fun `setStarred surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.setStarred(mailboxId = "mb1", emailId = "e1", starred = false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `markThreadRead posts to the thread read endpoint`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.markThreadRead(mailboxId = "mb1", threadId = "t1")

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/threads/t1/read", request.url.encodedPath)
    }

    @Test
    fun `markThreadRead surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.markThreadRead(mailboxId = "mb1", threadId = "t1")

        assertTrue(result.isFailure)
    }

    // --- labels --------------------------------------------------------------------------------

    private val labelledPage = """
        {"emails":[{"id":"e1","subject":"S","sender":"a@x.dev","recipient":"me@x.dev","date":"2026-01-01T00:00:00Z",
          "labels":[{"id":"work","name":"Work","color":"blue"},{"id":"fun","name":"Fun","color":"red"}]}],"totalCount":1}
    """.trimIndent()

    private val workLabel = Label("work", "Work", "blue")
    private val funLabel = Label("fun", "Fun", "red")

    @Test
    fun `getEmails maps the labels on each email`() = runTest {
        val engine = MockEngine { respond(labelledPage, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.getEmails("mb1", "inbox", page = 1, limit = 25).getOrThrow()

        assertEquals(listOf(workLabel, funLabel), page.emails.single().labels)
    }

    @Test
    fun `a server that sends no labels field gives an email with no labels`() = runTest {
        val engine = MockEngine {
            respond(
                """{"emails":[{"id":"e1","subject":"S","sender":"a@x.dev","recipient":"me@x.dev","date":"2026-01-01T00:00:00Z"}],"totalCount":1}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.getEmails("mb1", "inbox", page = 1, limit = 25).getOrThrow()

        assertTrue(page.emails.single().labels.isEmpty())
    }

    @Test
    fun `labels survive the offline cache round trip`() = runTest {
        var online = true
        val engine = MockEngine {
            if (online) respond(labelledPage, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else respond("", HttpStatusCode.InternalServerError)
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        repository.getEmails("mb1", "inbox", page = 1, limit = 25)

        online = false
        val cached = repository.getEmails("mb1", "inbox", page = 1, limit = 25).getOrThrow()

        assertEquals(listOf(workLabel, funLabel), cached.emails.single().labels)
    }

    @Test
    fun `getEmailsByLabel asks for the label and does not touch the folder cache`() = runTest {
        val engine = MockEngine { respond(labelledPage, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val dao = FakeEmailDao()
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        val page = repository.getEmailsByLabel("mb1", "work", page = 2, limit = 25).getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/mailboxes/mb1/emails", request.url.encodedPath)
        assertEquals("work", request.url.parameters["label"])
        assertEquals("2", request.url.parameters["page"])
        assertEquals(1, page.totalCount)
        assertTrue(dao.getForFolder("mb1", "inbox").isEmpty())
    }

    @Test
    fun `bulkUpdate sends the label id for label and unlabel`() = runTest {
        for ((action, name) in mapOf(BulkAction.LABEL to "label", BulkAction.UNLABEL to "unlabel")) {
            val engine = bulkEngine()
            EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
                .bulkUpdate("mb1", action, listOf("e1"), labelId = "work")

            val body = bodyOf(engine.requestHistory.single())
            assertTrue(body.contains("\"action\":\"$name\"") && body.contains("\"labelId\":\"work\""), body)
        }
    }

    // --- bulk actions --------------------------------------------------------------------------

    private val bulkOk = """{"count":2}"""

    /** Answers each request with the next status (OK once the list runs out). */
    private fun bulkEngine(vararg statuses: HttpStatusCode): MockEngine {
        var call = 0
        return MockEngine { _ ->
            val status = statuses.getOrElse(call++) { HttpStatusCode.OK }
            respond(
                content = if (status == HttpStatusCode.OK) bulkOk else "",
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }

    private fun bodyOf(request: io.ktor.client.request.HttpRequestData): String = (request.body as TextContent).text

    @Test
    fun `bulkUpdate sends one request with the action and every id`() = runTest {
        val engine = bulkEngine()
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val outcome = repository.bulkUpdate("mb1", BulkAction.READ, listOf("e1", "e2"))

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/bulk", request.url.encodedPath)
        val body = bodyOf(request)
        assertTrue(body.contains("\"action\":\"read\""), body)
        assertTrue(body.contains("\"ids\":[\"e1\",\"e2\"]"), body)
        assertEquals(setOf("e1", "e2"), outcome.succeeded)
        assertTrue(outcome.failed.isEmpty())
    }

    @Test
    fun `bulkUpdate names the destination folder for a move`() = runTest {
        val engine = bulkEngine()
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        repository.bulkUpdate("mb1", BulkAction.MOVE, listOf("e1"), folderId = "archive")

        val body = bodyOf(engine.requestHistory.single())
        assertTrue(body.contains("\"action\":\"move\""), body)
        assertTrue(body.contains("\"folderId\":\"archive\""), body)
    }

    @Test
    fun `bulkUpdate maps each action to the server's action name`() = runTest {
        val names = mapOf(
            BulkAction.READ to "read", BulkAction.UNREAD to "unread", BulkAction.STAR to "star",
            BulkAction.UNSTAR to "unstar", BulkAction.MOVE to "move", BulkAction.DELETE to "delete",
        )
        for ((action, name) in names) {
            val engine = bulkEngine()
            EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
                .bulkUpdate("mb1", action, listOf("e1"), folderId = "archive".takeIf { action == BulkAction.MOVE })

            assertTrue(bodyOf(engine.requestHistory.single()).contains("\"action\":\"$name\""), "action $action")
        }
    }

    @Test
    fun `bulkUpdate splits more than 200 ids into separate requests`() = runTest {
        val engine = bulkEngine()
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        val ids = (1..450).map { "e$it" }

        val outcome = repository.bulkUpdate("mb1", BulkAction.DELETE, ids)

        assertEquals(3, engine.requestHistory.size)
        assertEquals(ids.toSet(), outcome.succeeded)
    }

    @Test
    fun `a failing chunk reports only its own ids as failed`() = runTest {
        val engine = bulkEngine(HttpStatusCode.OK, HttpStatusCode.InternalServerError)
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        val ids = (1..250).map { "e$it" }

        val outcome = repository.bulkUpdate("mb1", BulkAction.DELETE, ids)

        assertEquals(ids.take(200).toSet(), outcome.succeeded)
        assertEquals(ids.drop(200).toSet(), outcome.failed)
    }

    @Test
    fun `bulkUpdate with no ids makes no request`() = runTest {
        val engine = bulkEngine()
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val outcome = repository.bulkUpdate("mb1", BulkAction.READ, emptyList())

        assertTrue(engine.requestHistory.isEmpty())
        assertTrue(outcome.succeeded.isEmpty() && outcome.failed.isEmpty())
    }

    private fun composeRequest() = ComposeEmailRequest(
        fromEmail = "me@example.dev",
        fromName = "Me",
        to = listOf("alice@example.dev"),
        cc = listOf("bob@example.dev"),
        subject = "Hello",
        body = "Hi there",
    )

    @Test
    fun `sendEmail posts to the emails endpoint`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.sendEmail(mailboxId = "mb1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails", request.url.encodedPath)
    }

    @Test
    fun `sendEmail always sends an explicit disposition for each attachment`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e1","status":"sent"}""",
                status = HttpStatusCode.Accepted,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        val withFile = composeRequest().copy(
            attachments = listOf(ComposeAttachment("a.txt", "text/plain", "hi".encodeToByteArray())),
        )

        repository.sendEmail(mailboxId = "mb1", request = withFile)

        // The server's schema requires `disposition`; encodeDefaults = false would drop it when it
        // equals its default, and the send would be rejected with a 400.
        val body = (engine.requestHistory.single().body as TextContent).text
        assertTrue(body.contains("\"disposition\":\"attachment\""), "body was $body")
    }

    @Test
    fun `sendEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.sendEmail(mailboxId = "mb1", request = composeRequest())

        assertTrue(result.isFailure)
    }

    @Test
    fun `replyEmail posts to the reply endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e2","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.replyEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/reply", request.url.encodedPath)
    }

    @Test
    fun `replyEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.replyEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isFailure)
    }

    @Test
    fun `forwardEmail posts to the forward endpoint for the given email`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"id":"e3","status":"sent"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.forwardEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isSuccess)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/mailboxes/mb1/emails/e1/forward", request.url.encodedPath)
    }

    @Test
    fun `forwardEmail surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.forwardEmail(mailboxId = "mb1", emailId = "e1", request = composeRequest())

        assertTrue(result.isFailure)
    }

    @Test
    fun `search sends the query text and every filter as request parameters`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"emails":[],"totalCount":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        val query = SearchQuery(
            query = "invoice",
            folder = "inbox",
            from = "alice@example.dev",
            to = "me@example.dev",
            subject = "receipt",
            dateStart = "2026-01-01",
            dateEnd = "2026-07-01",
            isRead = false,
            isStarred = true,
            hasAttachment = true,
        )

        repository.search(mailboxId = "mb1", query = query, page = 2, limit = 25)

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/mailboxes/mb1/search", request.url.encodedPath)
        assertEquals("invoice", request.url.parameters["query"])
        assertEquals("inbox", request.url.parameters["folder"])
        assertEquals("alice@example.dev", request.url.parameters["from"])
        assertEquals("me@example.dev", request.url.parameters["to"])
        assertEquals("receipt", request.url.parameters["subject"])
        assertEquals("2026-01-01", request.url.parameters["date_start"])
        assertEquals("2026-07-01", request.url.parameters["date_end"])
        assertEquals("false", request.url.parameters["is_read"])
        assertEquals("true", request.url.parameters["is_starred"])
        assertEquals("true", request.url.parameters["has_attachment"])
        assertEquals("2", request.url.parameters["page"])
        assertEquals("25", request.url.parameters["limit"])
    }

    @Test
    fun `search omits unset filters from the request`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"emails":[],"totalCount":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        repository.search(mailboxId = "mb1", query = SearchQuery(query = "invoice"), page = 1, limit = 50)

        val request = engine.requestHistory.single()
        assertEquals(null, request.url.parameters["folder"])
        assertEquals(null, request.url.parameters["from"])
        assertEquals(null, request.url.parameters["is_read"])
        assertEquals(null, request.url.parameters["has_attachment"])
    }

    @Test
    fun `search maps the page dto into the domain EmailPage`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "emails": [
                            {
                                "id": "e1",
                                "subject": "Invoice",
                                "sender": "a@example.dev",
                                "recipient": "b@example.dev",
                                "date": "2026-07-16T00:00:00Z",
                                "read": true,
                                "starred": false,
                                "thread_id": "t1",
                                "folder_id": "inbox"
                            }
                        ],
                        "totalCount": 1
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.search(mailboxId = "mb1", query = SearchQuery(query = "invoice"), page = 1, limit = 50).getOrThrow()

        assertEquals(1, page.totalCount)
        assertEquals("e1", page.emails.single().id)
    }

    @Test
    fun `search surfaces the failure instead of throwing`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.search(mailboxId = "mb1", query = SearchQuery(query = "invoice"), page = 1, limit = 50)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getEmails caches the first page on success`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "emails": [
                            {"id":"e1","subject":"Hello","sender":"a@example.dev","recipient":"b@example.dev","date":"2026-07-16T00:00:00Z","read":false,"starred":false,"thread_id":"t1","folder_id":"inbox"}
                        ],
                        "totalCount": 1
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dao = FakeEmailDao()
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertEquals(listOf("e1"), dao.getForFolder("mb1", "inbox").map { it.id })
    }

    @Test
    fun `getEmails does not cache pages beyond the first`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"emails":[{"id":"e2","subject":"s","sender":"a","recipient":"b","date":"2026-07-16T00:00:00Z","read":false,"starred":false}],"totalCount":2}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dao = FakeEmailDao()
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 2, limit = 50)

        assertTrue(dao.getForFolder("mb1", "inbox").isEmpty())
    }

    @Test
    fun `getEmails replaces stale cached rows for the folder on success`() = runTest {
        val dao = FakeEmailDao()
        dao.upsertAll(listOf(EmailEntity(mailboxId = "mb1", id = "stale", folderId = "inbox", subject = "s", sender = "a", recipient = "b", date = "2026-01-01T00:00:00Z", read = false, starred = false, threadId = null, snippet = null, threadUnreadCount = 0)))
        val engine = MockEngine { _ ->
            respond(
                content = """{"emails":[{"id":"e1","subject":"s","sender":"a","recipient":"b","date":"2026-07-16T00:00:00Z","read":false,"starred":false}],"totalCount":1}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertEquals(listOf("e1"), dao.getForFolder("mb1", "inbox").map { it.id })
    }

    @Test
    fun `getEmails falls back to the cache when the network call fails on the first page`() = runTest {
        val dao = FakeEmailDao()
        dao.upsertAll(listOf(EmailEntity(mailboxId = "mb1", id = "e1", folderId = "inbox", subject = "Cached", sender = "a", recipient = "b", date = "2026-07-16T00:00:00Z", read = true, starred = false, threadId = null, snippet = null, threadUnreadCount = 0)))
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        val result = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertEquals("Cached", result.getOrThrow().emails.single().subject)
    }

    @Test
    fun `getEmails surfaces the original failure when the network fails and the cache is empty`() = runTest {
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val result = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 50)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getEmails does not fall back to the cache for pages beyond the first`() = runTest {
        val dao = FakeEmailDao()
        dao.upsertAll(listOf(EmailEntity(mailboxId = "mb1", id = "e1", folderId = "inbox", subject = "Cached", sender = "a", recipient = "b", date = "2026-07-16T00:00:00Z", read = true, starred = false, threadId = null, snippet = null, threadUnreadCount = 0)))
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        val result = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 2, limit = 50)

        assertTrue(result.isFailure)
    }

    @Test
    fun `clearCache empties the cached email table for every mailbox and folder`() = runTest {
        val dao = FakeEmailDao()
        dao.upsertAll(
            listOf(
                EmailEntity(mailboxId = "mb1", id = "e1", folderId = "inbox", subject = "A", sender = "a", recipient = "b", date = "2026-07-16T00:00:00Z", read = true, starred = false, threadId = null, snippet = null, threadUnreadCount = 0),
                EmailEntity(mailboxId = "mb2", id = "e2", folderId = "sent", subject = "B", sender = "a", recipient = "b", date = "2026-07-16T00:00:00Z", read = true, starred = false, threadId = null, snippet = null, threadUnreadCount = 0),
            ),
        )
        val engine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        repository.clearCache()

        assertTrue(dao.getForFolder("mb1", "inbox").isEmpty())
        assertTrue(dao.getForFolder("mb2", "sent").isEmpty())
    }

    // --- conversations ---------------------------------------------------------------------

    private val threadedPage = """
        {"emails":[
          {"id":"e2","subject":"Plans","sender":"a@x.dev","recipient":"me@x.dev","date":"2026-01-02T00:00:00Z","thread_id":"t1",
           "thread_count":3,"thread_unread_count":1,"participants":"a@x.dev, me@x.dev"},
          {"id":"e9","subject":"Solo","sender":"b@x.dev","recipient":"me@x.dev","date":"2026-01-01T00:00:00Z","thread_id":"t9"}
        ],"totalCount":2}
    """.trimIndent()

    private fun okJson(body: String) = MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }

    @Test
    fun `a folder list asks the server for one row per conversation`() = runTest {
        val engine = okJson(threadedPage)

        EmailRepositoryImpl(apiFor(engine), FakeEmailDao()).getEmails("mb1", "inbox", page = 1, limit = 25)

        assertEquals("true", engine.requestHistory.single().url.parameters["threaded"])
    }

    @Test
    fun `the conversation size and unread count reach the row`() = runTest {
        val page = EmailRepositoryImpl(apiFor(okJson(threadedPage)), FakeEmailDao()).getEmails("mb1", "inbox", 1, 25).getOrThrow()

        val (conversation, solo) = page.emails
        assertEquals(3, conversation.threadCount)
        assertEquals(1, conversation.threadUnreadCount)
        assertTrue(conversation.isUnread() || conversation.read)
        assertEquals(1, solo.threadCount)
    }

    @Test
    fun `conversation size survives the offline cache`() = runTest {
        var online = true
        val engine = MockEngine {
            if (online) respond(threadedPage, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else respond("", HttpStatusCode.InternalServerError)
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
        repository.getEmails("mb1", "inbox", 1, 25)

        online = false
        val cached = repository.getEmails("mb1", "inbox", 1, 25).getOrThrow()

        assertEquals(listOf(3, 1), cached.emails.sortedByDescending { it.date }.map { it.threadCount })
    }

    @Test
    fun `search groups its results by conversation`() = runTest {
        val engine = okJson(threadedPage)

        EmailRepositoryImpl(apiFor(engine), FakeEmailDao()).search("mb1", SearchQuery(query = "plans"), page = 1, limit = 25)

        assertEquals("thread", engine.requestHistory.single().url.parameters["group"])
    }

    @Test
    fun `bulkUpdate sends conversation rows by thread and the rest by id`() = runTest {
        val engine = bulkEngine()

        EmailRepositoryImpl(apiFor(engine), FakeEmailDao()).bulkUpdate(
            "mb1", BulkAction.MOVE, listOf("e2", "e9"),
            folderId = "archive",
            conversations = mapOf("e2" to "t1"),
            folder = "inbox",
        )

        val body = Json.parseToJsonElement(bodyOf(engine.requestHistory.single())).jsonObject
        assertEquals(listOf("e9"), body["ids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("t1"), body["thread_ids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("inbox", body["folder"]!!.jsonPrimitive.content)
        assertEquals("archive", body["folderId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `without conversations the request carries no thread fields`() = runTest {
        val engine = bulkEngine()

        EmailRepositoryImpl(apiFor(engine), FakeEmailDao()).bulkUpdate("mb1", BulkAction.READ, listOf("e1"), folder = "inbox")

        val body = bodyOf(engine.requestHistory.single())
        assertTrue("thread_ids" !in body && "\"folder\"" !in body, body)
    }

    @Test
    fun `a conversation row's outcome is reported under the row's own id`() = runTest {
        val engine = bulkEngine(HttpStatusCode.InternalServerError)

        val outcome = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())
            .bulkUpdate("mb1", BulkAction.DELETE, listOf("e2"), conversations = mapOf("e2" to "t1"), folder = "inbox")

        assertEquals(setOf("e2"), outcome.failed)
    }

    // --- domain-wide "All inboxes" ------------------------------------------------------------

    private fun json(content: String) = MockEngine { _ ->
        respond(content, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private val domainPage = """
        {
            "emails": [
                {"id":"e1","mailboxId":"a@example.com","subject":"Newer","sender":"x@y.dev","recipient":"a@example.com","date":"2026-07-10T10:00:00Z","folder_id":"inbox"},
                {"id":"e2","mailboxId":"b@example.com","subject":"Older","sender":"x@y.dev","recipient":"b@example.com","date":"2026-07-09T10:00:00Z","folder_id":"inbox"}
            ],
            "totalCount": 9,
            "nextCursor": "CUR1"
        }
    """.trimIndent()

    @Test
    fun `getDomainEmails asks the domain endpoint for one conversation per row`() = runTest {
        val engine = json(domainPage)
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        repository.getDomainEmails(domain = "example.com", folder = "inbox", cursor = null, limit = 50)

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/domains/example.com/emails", request.url.encodedPath)
        assertEquals("inbox", request.url.parameters["folder"])
        assertEquals("true", request.url.parameters["threaded"])
        assertEquals("50", request.url.parameters["limit"])
        assertEquals(null, request.url.parameters["cursor"])
    }

    @Test
    fun `getDomainEmails sends the cursor for a later page`() = runTest {
        val engine = json(domainPage)
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        repository.getDomainEmails(domain = "example.com", folder = "inbox", cursor = "CUR0", limit = 50)

        assertEquals("CUR0", engine.requestHistory.single().url.parameters["cursor"])
    }

    @Test
    fun `getDomainEmails maps each row's mailbox, the total and the next cursor`() = runTest {
        val repository = EmailRepositoryImpl(apiFor(json(domainPage)), FakeEmailDao())

        val page = repository.getDomainEmails("example.com", "inbox", cursor = null, limit = 50).getOrThrow()

        assertEquals(listOf("a@example.com", "b@example.com"), page.emails.map { it.mailboxId })
        assertEquals(9, page.totalCount)
        assertEquals("CUR1", page.nextCursor)
    }

    @Test
    fun `getDomainEmails caches the first page under each row's own mailbox, in the same call`() = runTest {
        val dao = FakeEmailDao()
        val engine = json(domainPage)
        val repository = EmailRepositoryImpl(apiFor(engine), dao)

        repository.getDomainEmails("example.com", "inbox", cursor = null, limit = 50)

        assertEquals(1, engine.requestHistory.size)
        assertEquals(listOf("e1"), dao.getForFolder("a@example.com", "inbox").map { it.id })
        assertEquals(listOf("e2"), dao.getForFolder("b@example.com", "inbox").map { it.id })
    }

    @Test
    fun `getDomainEmails does not cache later pages`() = runTest {
        val dao = FakeEmailDao()
        val repository = EmailRepositoryImpl(apiFor(json(domainPage)), dao)

        repository.getDomainEmails("example.com", "inbox", cursor = "CUR0", limit = 50)

        assertTrue(dao.getForFolder("a@example.com", "inbox").isEmpty())
    }

    @Test
    fun `getDomainEmails offline serves the domain's cached rows newest first, and only that domain's`() = runTest {
        val dao = FakeEmailDao()
        fun entity(id: String, mailbox: String, date: String) = com.sonicstarsolutions.agentic.inbox.data.local.EmailEntity(
            mailboxId = mailbox, id = id, folderId = "inbox", subject = id, sender = "s", recipient = "r",
            date = date, read = false, starred = false, threadId = null, snippet = null, threadUnreadCount = 0,
        )
        dao.upsertAll(listOf(
            entity("old", "a@example.com", "2026-07-01T00:00:00Z"),
            entity("new", "b@example.com", "2026-07-09T00:00:00Z"),
            entity("elsewhere", "c@other.org", "2026-07-20T00:00:00Z"),
        ))
        val offline = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(offline), dao)

        val page = repository.getDomainEmails("example.com", "inbox", cursor = null, limit = 50).getOrThrow()

        assertEquals(listOf("new", "old"), page.emails.map { it.id })
        assertEquals(listOf("b@example.com", "a@example.com"), page.emails.map { it.mailboxId })
        assertEquals(null, page.nextCursor)
    }

    @Test
    fun `getDomainEmails does not fall back to the cache for a later page`() = runTest {
        val offline = MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }
        val repository = EmailRepositoryImpl(apiFor(offline), FakeEmailDao())

        assertTrue(repository.getDomainEmails("example.com", "inbox", cursor = "CUR0", limit = 50).isFailure)
    }

    @Test
    fun `searchDomain sends the filters, groups by conversation and pages by cursor`() = runTest {
        val engine = json(domainPage)
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.searchDomain(
            domain = "example.com",
            query = SearchQuery(query = "invoice", from = "acme", isRead = false),
            cursor = "CUR0",
            limit = 30,
        ).getOrThrow()

        val request = engine.requestHistory.single()
        assertEquals("/api/v1/domains/example.com/search", request.url.encodedPath)
        assertEquals("invoice", request.url.parameters["query"])
        assertEquals("acme", request.url.parameters["from"])
        assertEquals("false", request.url.parameters["is_read"])
        assertEquals("thread", request.url.parameters["group"])
        assertEquals("CUR0", request.url.parameters["cursor"])
        assertEquals("30", request.url.parameters["limit"])
        assertEquals("CUR1", page.nextCursor)
        assertEquals("a@example.com", page.emails.first().mailboxId)
    }

    @Test
    fun `a single-mailbox list stamps its own mailbox on every row, live and cached`() = runTest {
        val dao = FakeEmailDao()
        val repository = EmailRepositoryImpl(apiFor(json("""{"emails":[{"id":"e1","subject":"s","sender":"a","recipient":"b","date":"2026-07-10T10:00:00Z"}],"totalCount":1}""")), dao)

        val live = repository.getEmails("mb1", "inbox", page = 1, limit = 50).getOrThrow()
        assertEquals("mb1", live.emails.single().mailboxId)

        val offline = EmailRepositoryImpl(apiFor(MockEngine { _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }), dao)
        assertEquals("mb1", offline.getEmails("mb1", "inbox", page = 1, limit = 50).getOrThrow().emails.single().mailboxId)
    }

    @Test
    fun `a list row shows the sender name the server sends`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"emails":[{"id":"e1","subject":"Hi","sender":"dave@example.net","sender_name":"Dave Lee","recipient":"me@example.dev","date":"2026-07-16T00:00:00Z"}],"totalCount":1}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = EmailRepositoryImpl(apiFor(engine), FakeEmailDao())

        val page = repository.getEmails(mailboxId = "mb1", folder = "inbox", page = 1, limit = 25).getOrThrow()

        assertEquals("Dave Lee <dave@example.net>", page.emails.single().sender)
    }
}
