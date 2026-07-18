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

    private val LINE_BREAK_TAG = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val PARAGRAPH_END_TAG =
        Regex("""</(p|div|li|tr|h[1-6]|blockquote)\s*>""", RegexOption.IGNORE_CASE)
    private val HTML_TAG = Regex("""</?[a-zA-Z][^>]*>""")
    // "&lt;div", "&amp;lt;div", … — markup hiding behind one or more layers of entity escaping,
    // which real drafts accumulate (each edit-send round-trip of an HTML draft escaped it again).
    private val ESCAPED_TAG = Regex("""&(amp;)*lt;/?[a-zA-Z]""", RegexOption.IGNORE_CASE)
    private val HORIZONTAL_WHITESPACE = Regex("""[ \t ]+""")
    private val SPACE_AROUND_NEWLINE = Regex(""" ?\n ?""")
    private val EXCESS_BLANK_LINES = Regex("""\n{3,}""")

    fun toPlainText(html: String): String {
        var text = SCRIPT_OR_STYLE_BLOCK.replace(html, "")
        text = BLOCK_BOUNDARY_TAG.replace(text, " ")
        text = ANY_TAG.replace(text, "")
        for ((entity, replacement) in ENTITIES) {
            text = text.replace(entity, replacement)
        }
        return WHITESPACE.replace(text, " ").trim()
    }

    /** Whether [text] carries markup — raw or entity-escaped — that [toEditableText] should
     * convert before the body goes into a plain-text editor. Comparison operators in prose
     * ("5 < 6") don't count: a tag needs a letter right after the bracket. */
    fun containsHtml(text: String): Boolean =
        HTML_TAG.containsMatchIn(text) || ESCAPED_TAG.containsMatchIn(text)

    /**
     * Reduces HTML to plain text fit for *editing*, unlike [toPlainText] which flattens
     * everything to one line for list snippets: line structure survives (`<br>` and closing
     * block tags become newlines), entities are decoded, and the strip-decode pass repeats
     * until stable so markup buried under stacked entity escaping (see [ESCAPED_TAG]) is
     * fully unwrapped rather than surfacing as literal tags. Plain text passes through with
     * its newlines untouched.
     */
    fun toEditableText(html: String): String {
        var text = html
        // Bounded: each pass strips one escaping layer; real content has needed at most a few.
        repeat(4) {
            val before = text
            text = SCRIPT_OR_STYLE_BLOCK.replace(text, "")
            text = LINE_BREAK_TAG.replace(text, "\n")
            text = PARAGRAPH_END_TAG.replace(text, "\n")
            text = ANY_TAG.replace(text, "")
            for ((entity, replacement) in ENTITIES) {
                text = text.replace(entity, replacement)
            }
            if (text == before) return@repeat
        }
        text = HORIZONTAL_WHITESPACE.replace(text, " ")
        text = SPACE_AROUND_NEWLINE.replace(text, "\n")
        text = EXCESS_BLANK_LINES.replace(text, "\n\n")
        return text.trim()
    }
}
