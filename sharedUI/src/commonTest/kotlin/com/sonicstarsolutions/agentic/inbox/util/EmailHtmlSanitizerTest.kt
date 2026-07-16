package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailHtmlSanitizerTest {

    @Test
    fun `remote image src is neutralized to data-src when images are not allowed`() {
        val html = """<p>Hi</p><img src="https://tracker.example.com/pixel.gif" width="1">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)

        assertTrue(result.contains("""data-src="https://tracker.example.com/pixel.gif""""), result)
        assertFalse(Regex("""(?<!data-)src="https://tracker""").containsMatchIn(result), result)
        assertTrue(result.contains("<p>Hi</p>"), result)
    }

    @Test
    fun `remote image src is left untouched when images are allowed`() {
        val html = """<img src="https://example.com/photo.png">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = true)

        assertEquals(html, result)
    }

    @Test
    fun `protocol-relative image sources are also neutralized`() {
        val html = """<img src="//example.com/photo.png">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)

        assertTrue(result.contains("""data-src="//example.com/photo.png""""), result)
    }

    @Test
    fun `inline data uri images are never neutralized`() {
        val html = """<img src="data:image/png;base64,iVBORw0KGgo=">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)

        assertEquals(html, result)
    }

    @Test
    fun `cid attachment references are never neutralized`() {
        val html = """<img src="cid:logo123">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)

        assertEquals(html, result)
    }

    @Test
    fun `multiple images in the same document are all neutralized`() {
        val html = """<img src="https://a.example.com/1.png"><img src="https://b.example.com/2.png">"""

        val result = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)

        assertEquals(2, Regex("data-src=").findAll(result).count())
        assertEquals(0, Regex("(?<!data-)src=\"https").findAll(result).count())
    }

    @Test
    fun `script tags are always stripped regardless of allowRemoteImages`() {
        val html = """<p>Hello</p><script>alert('hi')</script><p>Bye</p>"""

        val blocked = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false)
        val allowed = EmailHtmlSanitizer.sanitize(html, allowRemoteImages = true)

        assertFalse(blocked.contains("<script", ignoreCase = true), blocked)
        assertFalse(allowed.contains("<script", ignoreCase = true), allowed)
        assertTrue(blocked.contains("<p>Hello</p>"), blocked)
        assertTrue(blocked.contains("<p>Bye</p>"), blocked)
    }

    @Test
    fun `plain content without images or scripts passes through unchanged`() {
        val html = "<p>Just some text with no images.</p>"

        assertEquals(html, EmailHtmlSanitizer.sanitize(html, allowRemoteImages = false))
        assertEquals(html, EmailHtmlSanitizer.sanitize(html, allowRemoteImages = true))
    }
}
