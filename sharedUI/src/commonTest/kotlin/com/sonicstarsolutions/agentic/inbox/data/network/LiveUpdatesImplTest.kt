package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LiveUpdatesImplTest {

    // --- what the server says --------------------------------------------------------------

    @Test
    fun `each change scope the server sends is understood`() {
        assertEquals(LiveScope.EMAILS, parseLiveScope("""{"type":"changed","scope":"emails"}"""))
        assertEquals(LiveScope.FOLDERS, parseLiveScope("""{"type":"changed","scope":"folders"}"""))
        assertEquals(LiveScope.SCHEDULED, parseLiveScope("""{"type":"changed","scope":"scheduled"}"""))
        assertEquals(LiveScope.ALL, parseLiveScope("""{"type":"changed","scope":"all"}"""))
    }

    @Test
    fun `a scope from a newer server is treated as everything changed`() {
        assertEquals(LiveScope.ALL, parseLiveScope("""{"type":"changed","scope":"contacts"}"""))
        assertEquals(LiveScope.ALL, parseLiveScope("""{"type":"changed"}"""))
    }

    @Test
    fun `anything that is not a change notice is ignored`() {
        assertNull(parseLiveScope("""{"type":"pong"}"""))
        assertNull(parseLiveScope("not json"))
        assertNull(parseLiveScope("[1,2]"))
        assertNull(parseLiveScope(""))
    }

    @Test
    fun `the wait before reconnecting doubles from one second up to thirty`() {
        assertEquals(listOf(1, 2, 4, 8, 16, 30, 30), (0..6).map { reconnectDelay(it).inWholeSeconds.toInt() })
        assertEquals(30, reconnectDelay(500).inWholeSeconds)
    }

    // --- the connection loop ---------------------------------------------------------------

    private class FakeConnector(private val script: suspend (attempt: Int, onText: suspend (String) -> Unit) -> Unit) : SocketConnector {
        var connects = 0
        val mailboxes = mutableListOf<String>()

        override suspend fun connect(mailboxId: String, onText: suspend (String) -> Unit) {
            mailboxes += mailboxId
            script(connects++, onText)
        }
    }

    private val emails = """{"type":"changed","scope":"emails"}"""

    @Test
    fun `messages from the socket are delivered as scopes, in order`() = runTest {
        val connector = FakeConnector { _, onText ->
            onText(emails)
            onText("""{"type":"changed","scope":"folders"}""")
            onText("noise")
            awaitCancellation()
        }
        val received = mutableListOf<LiveScope>()
        val job = launch { LiveUpdatesImpl(connector).changes("mb1").collect { received += it } }
        runCurrent()

        assertEquals(listOf(LiveScope.EMAILS, LiveScope.FOLDERS), received)
        assertEquals(listOf("mb1"), connector.mailboxes)
        job.cancel()
    }

    @Test
    fun `a dropped connection is reopened after a growing wait`() = runTest {
        val connector = FakeConnector { _, _ -> /* closes straight away */ }
        val job = launch { LiveUpdatesImpl(connector).changes("mb1").collect { } }

        runCurrent()
        assertEquals(1, connector.connects)
        advanceTimeBy(1.seconds - 1.milliseconds); runCurrent()
        assertEquals(1, connector.connects)
        advanceTimeBy(2.milliseconds); runCurrent()
        assertEquals(2, connector.connects)
        // The second wait is two seconds.
        advanceTimeBy(1.seconds + 500.milliseconds); runCurrent()
        assertEquals(2, connector.connects)
        advanceTimeBy(1.seconds); runCurrent()
        assertEquals(3, connector.connects)
        job.cancel()
    }

    @Test
    fun `a failed connection attempt is retried the same way`() = runTest {
        val connector = FakeConnector { attempt, _ -> if (attempt < 2) throw RuntimeException("handshake failed") }
        val job = launch { LiveUpdatesImpl(connector).changes("mb1").collect { } }

        runCurrent()
        advanceTimeBy(1.seconds + 1.milliseconds); runCurrent()
        advanceTimeBy(2.seconds + 1.milliseconds); runCurrent()

        assertEquals(3, connector.connects)
        job.cancel()
    }

    @Test
    fun `a delivered message resets the wait to one second`() = runTest {
        // Attempts 0 and 1 drop silently (waits of 1s then 2s); attempt 2 delivers, then drops.
        val connector = FakeConnector { attempt, onText -> if (attempt == 2) onText(emails) }
        val job = launch { LiveUpdatesImpl(connector).changes("mb1").collect { } }

        runCurrent()
        advanceTimeBy(1.seconds + 1.milliseconds); runCurrent()   // -> attempt 1
        advanceTimeBy(2.seconds + 1.milliseconds); runCurrent()   // -> attempt 2, delivers, drops
        assertEquals(3, connector.connects)
        advanceTimeBy(1.seconds + 1.milliseconds); runCurrent()   // reset: back after 1s, not 4s

        assertEquals(4, connector.connects)
        job.cancel()
    }

    @Test
    fun `stopping the collector stops reconnecting`() = runTest {
        val connector = FakeConnector { _, _ -> }
        val job = launch { LiveUpdatesImpl(connector).changes("mb1").collect { } }
        runCurrent()

        job.cancel()
        advanceTimeBy(120.seconds); runCurrent()

        assertEquals(1, connector.connects)
    }
}
