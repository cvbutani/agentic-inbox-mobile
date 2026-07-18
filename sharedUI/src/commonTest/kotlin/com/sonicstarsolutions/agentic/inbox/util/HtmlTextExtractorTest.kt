package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextExtractorTest {

    @Test
    fun `plain text without tags passes through trimmed`() {
        assertEquals("Hello world", HtmlTextExtractor.toPlainText("  Hello world  "))
    }

    // -- toEditableText: HTML into a plain-text editor, line structure preserved ------------

    @Test
    fun `editable text keeps paragraphs as separate lines`() {
        assertEquals(
            "Hello\nWorld",
            HtmlTextExtractor.toEditableText("<p>Hello</p><p>World</p>"),
        )
    }

    @Test
    fun `editable text turns br tags into newlines`() {
        assertEquals(
            "Line one\nLine two",
            HtmlTextExtractor.toEditableText("Line one<br>Line two"),
        )
    }

    @Test
    fun `editable text keeps a quoted reply readable`() {
        val html = """<div style="white-space:pre-wrap">Sounds good.</div><br>""" +
            """<blockquote style="border-left: 2px solid #ccc;">On Fri, Alice wrote:<br><br>See you then.</blockquote>"""

        assertEquals(
            "Sounds good.\n\nOn Fri, Alice wrote:\n\nSee you then.",
            HtmlTextExtractor.toEditableText(html),
        )
    }

    @Test
    fun `editable text decodes entities`() {
        assertEquals(
            "A & B \"quoted\"",
            HtmlTextExtractor.toEditableText("A &amp; B &quot;quoted&quot;"),
        )
    }

    @Test
    fun `editable text cleans a draft that was escaped once per edit round-trip`() {
        // The regression this guards: each edit-send cycle of an HTML draft escaped it again,
        // so real drafts contained &amp;lt;div … — tags hiding behind stacked entity layers.
        val mangled = "&amp;lt;div style=&amp;quot;white-space:pre-wrap&amp;quot;&amp;gt;Got it&amp;lt;/div&amp;gt;"

        assertEquals("Got it", HtmlTextExtractor.toEditableText(mangled))
    }

    @Test
    fun `editable text collapses runs of blank lines to one`() {
        assertEquals(
            "One\n\nTwo",
            HtmlTextExtractor.toEditableText("<p>One</p><br><br><br><p>Two</p>"),
        )
    }

    @Test
    fun `editable text leaves plain multi-line text untouched`() {
        assertEquals(
            "Dear Bob,\n\nSee you then.",
            HtmlTextExtractor.toEditableText("Dear Bob,\n\nSee you then."),
        )
    }

    // -- containsHtml: whether a body needs converting before editing -----------------------

    @Test
    fun `containsHtml recognizes markup and escaped markup`() {
        assertEquals(true, HtmlTextExtractor.containsHtml("<div>Hi</div>"))
        assertEquals(true, HtmlTextExtractor.containsHtml("&lt;div&gt;Hi&lt;/div&gt;"))
        assertEquals(false, HtmlTextExtractor.containsHtml("Just words, 5 < 6, a & b"))
        assertEquals(false, HtmlTextExtractor.containsHtml("Dear Bob,\n\nSee you."))
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
