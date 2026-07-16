package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Renders [html] in a sandboxed platform WebView (Android WebView / iOS WKWebView) with
 * JavaScript disabled. Callers are expected to pass HTML already run through
 * [com.sonicstarsolutions.agentic.inbox.util.EmailHtmlSanitizer] and
 * [com.sonicstarsolutions.agentic.inbox.util.EmailHtmlDocumentBuilder] — this composable does no
 * sanitization or theming of its own.
 *
 * [backgroundColor] sets the WebView's native background (not just CSS) so there's no white
 * flash before the page's own background-color style takes effect.
 */
@Composable
expect fun HtmlBody(html: String, backgroundColor: Color, modifier: Modifier = Modifier)
