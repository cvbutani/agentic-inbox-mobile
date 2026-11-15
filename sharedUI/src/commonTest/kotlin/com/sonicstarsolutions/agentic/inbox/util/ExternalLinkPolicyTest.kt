package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalLinkPolicyTest {

    @Test
    fun `http and https links open externally`() {
        assertTrue(ExternalLinkPolicy.opensExternally("https://example.com/a?b=1"))
        assertTrue(ExternalLinkPolicy.opensExternally("http://example.com"))
    }

    @Test
    fun `mailto and tel links open externally`() {
        assertTrue(ExternalLinkPolicy.opensExternally("mailto:alice@example.dev"))
        assertTrue(ExternalLinkPolicy.opensExternally("tel:+15551234567"))
    }

    @Test
    fun `scheme matching is case insensitive`() {
        assertTrue(ExternalLinkPolicy.opensExternally("HTTPS://Example.com"))
    }

    @Test
    fun `script and file style schemes never open`() {
        assertFalse(ExternalLinkPolicy.opensExternally("javascript:alert(1)"))
        assertFalse(ExternalLinkPolicy.opensExternally("file:///sdcard/secret.txt"))
        assertFalse(ExternalLinkPolicy.opensExternally("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(ExternalLinkPolicy.opensExternally("data:text/html,<b>x</b>"))
    }

    @Test
    fun `blank, relative and in-page links do not open externally`() {
        assertFalse(ExternalLinkPolicy.opensExternally(""))
        assertFalse(ExternalLinkPolicy.opensExternally("   "))
        assertFalse(ExternalLinkPolicy.opensExternally("#section"))
        assertFalse(ExternalLinkPolicy.opensExternally("/relative/path"))
        assertFalse(ExternalLinkPolicy.opensExternally("about:blank"))
    }

    @Test
    fun `leading whitespace is ignored`() {
        assertEquals(true, ExternalLinkPolicy.opensExternally("  https://example.com"))
    }
}
