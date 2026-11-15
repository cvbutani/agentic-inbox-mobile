package com.sonicstarsolutions.agentic.inbox.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveSocketUrlTest {

    @Test
    fun `https becomes a secure websocket and the path names the mailbox`() {
        assertEquals(
            "wss://mail.example.dev/api/v1/mailboxes/hello@example.dev/events",
            liveSocketUrl("https://mail.example.dev", "hello@example.dev"),
        )
    }

    @Test
    fun `http becomes a plain websocket`() {
        // nosemgrep: javascript.lang.security.detect-insecure-websocket.detect-insecure-websocket
        assertEquals("ws://10.0.2.2:8787/api/v1/mailboxes/a@b.dev/events", liveSocketUrl("http://10.0.2.2:8787", "a@b.dev"))
    }

    @Test
    fun `scheme case and a trailing slash do not matter`() {
        assertEquals("wss://x.dev/api/v1/mailboxes/a@b.dev/events", liveSocketUrl("HTTPS://x.dev/", "a@b.dev"))
    }

    @Test
    fun `characters that are not path-safe in a mailbox id are encoded`() {
        assertEquals("wss://x.dev/api/v1/mailboxes/a%20b%2Fc@b.dev/events", liveSocketUrl("https://x.dev", "a b/c@b.dev"))
    }

    @Test
    fun `no base url means nothing to connect to`() {
        assertNull(liveSocketUrl("", "a@b.dev"))
    }
}
