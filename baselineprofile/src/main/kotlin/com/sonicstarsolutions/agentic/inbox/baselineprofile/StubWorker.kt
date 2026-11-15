package com.sonicstarsolutions.agentic.inbox.baselineprofile

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A stand-in for the `cloudflare/agentic-inbox` Worker, served from the test process on the
 * device's loopback interface.
 *
 * Profile generation needs to get past Onboarding into the inbox, and that takes a Worker that
 * answers. Pointing the app at this one keeps real credentials out of the test and still runs the
 * app's real Ktor, serialization and Room code, which is the code the profile should cover. The
 * responses mirror the shapes in `data/network/dto`; only the endpoints the journeys reach are
 * implemented, and everything else answers 404.
 */
class StubWorker {
    private val server = MockWebServer().apply { dispatcher = WorkerDispatcher() }

    /** Loopback by address rather than "localhost", to match the app's cleartext allowance. */
    val baseUrl: String get() = "http://$LOOPBACK:${server.port}"

    fun start() = server.start(InetAddress.getByName(LOOPBACK), 0)

    fun shutdown() = server.shutdown()

    private class WorkerDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.requestUrl?.encodedPath.orEmpty()
            val method = request.method.orEmpty()
            val page = request.requestUrl?.queryParameter("page")?.toIntOrNull() ?: 1

            val body: Any? = when {
                method == "GET" && path == "/api/v1/config" -> config()
                method == "GET" && path == "/api/v1/mailboxes" -> mailboxes()
                method == "GET" && MAILBOX.matches(path) -> mailbox(MAILBOX.find(path)!!.groupValues[1])
                method == "GET" && FOLDERS.matches(path) -> folders()
                method == "GET" && EMAILS.matches(path) -> emailPage(page)
                method == "GET" && SEARCH.matches(path) -> emailPage(page)
                method == "GET" && THREAD.matches(path) -> thread(THREAD.find(path)!!.groupValues[1])
                method == "GET" && EMAIL.matches(path) -> fullEmail(EMAIL.find(path)!!.groupValues[1].toIndex())
                method == "PUT" && EMAIL.matches(path) -> fullEmail(EMAIL.find(path)!!.groupValues[1].toIndex())
                method == "POST" && THREAD_READ.matches(path) -> JSONObject().put("status", "ok")
                else -> null
            }

            return if (body == null) {
                MockResponse().setResponseCode(404)
            } else {
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(body.toString())
            }
        }
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"

        /** Two full pages at the app's page size of 50, so scrolling reaches the load-more path. */
        const val PAGE_SIZE = 50
        const val TOTAL_EMAILS = 100

        val MAILBOX = Regex("/api/v1/mailboxes/([^/]+)")
        val FOLDERS = Regex("/api/v1/mailboxes/[^/]+/folders")
        val EMAILS = Regex("/api/v1/mailboxes/[^/]+/emails")
        val SEARCH = Regex("/api/v1/mailboxes/[^/]+/search")
        val EMAIL = Regex("/api/v1/mailboxes/[^/]+/emails/([^/]+)")
        val THREAD = Regex("/api/v1/mailboxes/[^/]+/threads/([^/]+)")
        val THREAD_READ = Regex("/api/v1/mailboxes/[^/]+/threads/[^/]+/read")

        val SENDERS = listOf(
            "Ada Lovelace <ada@example.com>",
            "Grace Hopper <grace@example.com>",
            "Alan Turing <alan@example.com>",
            "Katherine Johnson <katherine@example.com>",
        )

        fun String.toIndex(): Int = substringAfterLast('-').toIntOrNull() ?: 0

        fun config(): JSONObject = JSONObject()
            .put("domains", JSONArray(listOf("example.com")))
            .put("emailAddresses", JSONArray())

        fun mailboxes(): JSONArray = JSONArray()
            .put(mailbox("support@example.com"))
            .put(mailbox("sales@example.com"))

        fun mailbox(id: String): JSONObject = JSONObject()
            .put("id", id)
            .put("email", id)
            .put("name", id.substringBefore('@').replaceFirstChar { it.uppercase() })

        fun folders(): JSONArray = JSONArray().apply {
            listOf("inbox" to "Inbox", "draft" to "Drafts", "sent" to "Sent", "archive" to "Archive", "trash" to "Trash")
                .forEach { (id, name) ->
                    put(JSONObject().put("id", id).put("name", name).put("unreadCount", if (id == "inbox") 12 else 0))
                }
        }

        fun emailPage(page: Int): JSONObject {
            val first = (page - 1) * PAGE_SIZE
            val emails = JSONArray()
            for (index in first until minOf(first + PAGE_SIZE, TOTAL_EMAILS)) emails.put(metadata(index))
            return JSONObject().put("emails", emails).put("totalCount", TOTAL_EMAILS)
        }

        fun thread(threadId: String): JSONArray {
            val index = threadId.toIndex()
            return JSONArray().put(fullEmail(index)).put(fullEmail(index, reply = true))
        }

        fun metadata(index: Int): JSONObject = JSONObject()
            .put("id", "email-$index")
            .put("subject", subject(index))
            .put("sender", SENDERS[index % SENDERS.size])
            .put("recipient", "support@example.com")
            .put("date", Instant.now().minus(index.toLong(), ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString())
            .put("read", index % 3 != 0)
            .put("starred", index % 7 == 0)
            .put("thread_id", "thread-$index")
            .put("folder_id", "inbox")
            .put("snippet", "Following up on item $index from this week's review, with the details below.")
            .put("thread_unread_count", if (index % 3 == 0) 1 else 0)

        fun fullEmail(index: Int, reply: Boolean = false): JSONObject = metadata(index)
            .put("id", if (reply) "email-$index-reply" else "email-$index")
            .put("subject", if (reply) "Re: ${subject(index)}" else subject(index))
            .put("message_id", "<message-$index${if (reply) "-reply" else ""}@example.com>")
            .put("attachments", JSONArray())
            .put(
                "body",
                "<html><body><h2>${subject(index)}</h2>" +
                    "<p>Hello,</p>" +
                    "<p>Here is the summary for item $index. " +
                    "See <a href=\"https://example.com/items/$index\">the full write-up</a> for more.</p>" +
                    "<ul><li>First point</li><li>Second point</li><li>Third point</li></ul>" +
                    "<blockquote>Earlier message quoted here for context.</blockquote>" +
                    "<p>Thanks,<br>The team</p></body></html>",
            )

        fun subject(index: Int): String = "Weekly review item $index"
    }
}
