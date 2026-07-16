package com.sonicstarsolutions.agentic.inbox.util

/**
 * Prepares an email's raw HTML body for display in the sandboxed WebView used by ThreadScreen.
 * JS is disabled at the WebView level regardless (belt-and-braces defense), but `<script>` is
 * still stripped here; remote image loading is the main privacy concern (tracking pixels), so
 * remote `<img src>` is renamed to `data-src` unless the user has opted in for this message —
 * inline `data:` and attachment `cid:` sources are left alone since they don't leak anything.
 */
object EmailHtmlSanitizer {

    private val SCRIPT_TAG = Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val IMG_TAG = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val SRC_ATTR = Regex("""\bsrc(\s*=\s*)(["'])(.*?)\2""", RegexOption.IGNORE_CASE)

    fun sanitize(html: String, allowRemoteImages: Boolean): String {
        val withoutScripts = SCRIPT_TAG.replace(html, "")
        return if (allowRemoteImages) withoutScripts else neutralizeRemoteImages(withoutScripts)
    }

    private fun neutralizeRemoteImages(html: String): String =
        IMG_TAG.replace(html) { imgTag ->
            SRC_ATTR.replace(imgTag.value) { srcAttr ->
                val (equals, quote, url) = srcAttr.destructured
                if (isRemoteUrl(url)) "data-src$equals$quote$url$quote" else srcAttr.value
            }
        }

    private fun isRemoteUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("//")
}
