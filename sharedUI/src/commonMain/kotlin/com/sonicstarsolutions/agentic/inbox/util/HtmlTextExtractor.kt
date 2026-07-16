package com.sonicstarsolutions.agentic.inbox.util

/**
 * Reduces an email's HTML (body or list snippet) to plain text for contexts that can't render
 * markup — e.g. the inbox list preview, which is a plain Compose `Text()`, not a WebView.
 */
object HtmlTextExtractor {

    private val SCRIPT_OR_STYLE_BLOCK =
        Regex("""<(script|style)\b[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BLOCK_BOUNDARY_TAG =
        Regex("""</(p|div|li|tr|h[1-6])\s*>|<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val ANY_TAG = Regex("""<[^>]+>""")
    private val WHITESPACE = Regex("""\s+""")

    private val ENTITIES = mapOf(
        "&nbsp;" to " ",
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "'",
    )

    fun toPlainText(html: String): String {
        var text = SCRIPT_OR_STYLE_BLOCK.replace(html, "")
        text = BLOCK_BOUNDARY_TAG.replace(text, " ")
        text = ANY_TAG.replace(text, "")
        for ((entity, replacement) in ENTITIES) {
            text = text.replace(entity, replacement)
        }
        return WHITESPACE.replace(text, " ").trim()
    }
}
