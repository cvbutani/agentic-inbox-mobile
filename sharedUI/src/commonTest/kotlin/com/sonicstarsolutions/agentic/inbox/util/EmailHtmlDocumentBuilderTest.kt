package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertTrue

class EmailHtmlDocumentBuilderTest {

    @Test
    fun `wrap includes a mobile viewport meta tag so pages don't shrink to fit a desktop width`() {
        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = "<p>Hi</p>",
            textColorHex = "#111111",
            backgroundColorHex = "#ffffff",
            linkColorHex = "#0000ff",
        )

        assertTrue(result.contains("""name="viewport""""), result)
        assertTrue(result.contains("width=device-width"), result)
    }

    @Test
    fun `wrap applies the given background and text colors`() {
        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = "<p>Hi</p>",
            textColorHex = "#eeeeee",
            backgroundColorHex = "#121212",
            linkColorHex = "#8ab4f8",
        )

        assertTrue(result.contains("background-color: #121212"), result)
        assertTrue(result.contains("color: #eeeeee"), result)
    }

    @Test
    fun `wrap sets a readable explicit font size and line height`() {
        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = "<p>Hi</p>",
            textColorHex = "#111111",
            backgroundColorHex = "#ffffff",
            linkColorHex = "#0000ff",
        )

        assertTrue(result.contains("font-size: 16px"), result)
        assertTrue(result.contains("line-height"), result)
    }

    @Test
    fun `wrap colors links with the given link color`() {
        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = "<p>Hi</p>",
            textColorHex = "#111111",
            backgroundColorHex = "#ffffff",
            linkColorHex = "#8ab4f8",
        )

        assertTrue(Regex("""a\s*\{[^}]*color:\s*#8ab4f8""").containsMatchIn(result), result)
    }

    @Test
    fun `wrap embeds the original body html unchanged`() {
        val bodyHtml = """<div class="content"><p>Hello <b>world</b></p></div>"""

        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = bodyHtml,
            textColorHex = "#111111",
            backgroundColorHex = "#ffffff",
            linkColorHex = "#0000ff",
        )

        assertTrue(result.contains(bodyHtml), result)
    }

    @Test
    fun `wrap constrains images and tables to the viewport width`() {
        val result = EmailHtmlDocumentBuilder.wrap(
            bodyHtml = "<table width=\"600\"><tr><td>content</td></tr></table>",
            textColorHex = "#111111",
            backgroundColorHex = "#ffffff",
            linkColorHex = "#0000ff",
        )

        assertTrue(Regex("""(img|table)[^{]*\{[^}]*max-width:\s*100%""").containsMatchIn(result), result)
    }
}
