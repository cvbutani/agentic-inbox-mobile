package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextExtractorTest {

    @Test
    fun `plain text without tags passes through trimmed`() {
        assertEquals("Hello world", HtmlTextExtractor.toPlainText("  Hello world  "))
    }

    @Test
    fun `block level tags become spaces so paragraphs don't run together`() {
        assertEquals("Hello World", HtmlTextExtractor.toPlainText("<p>Hello</p><p>World</p>"))
    }

    @Test
    fun `inline tags are stripped without adding extra spacing`() {
        assertEquals(
            "Hi there, click here",
            HtmlTextExtractor.toPlainText("""Hi <b>there</b>, click <a href="https://example.com">here</a>"""),
        )
    }

    @Test
    fun `common html entities are decoded`() {
        assertEquals("A & B <test> \"quoted\" it's", HtmlTextExtractor.toPlainText("A &amp; B &lt;test&gt; &quot;quoted&quot; it&#39;s"))
    }

    @Test
    fun `nbsp entity becomes a regular space`() {
        assertEquals("Hello World", HtmlTextExtractor.toPlainText("Hello&nbsp;World"))
    }

    @Test
    fun `script and style blocks are removed along with their content`() {
        assertEquals(
            "Hello",
            HtmlTextExtractor.toPlainText("<script>alert('hi')</script><style>.a{color:red}</style>Hello"),
        )
    }

    @Test
    fun `repeated whitespace and newlines collapse to a single space`() {
        assertEquals("Hello World Test", HtmlTextExtractor.toPlainText("Hello\n\n\nWorld   Test"))
    }

    @Test
    fun `blank input produces an empty string`() {
        assertEquals("", HtmlTextExtractor.toPlainText("   "))
    }
}
