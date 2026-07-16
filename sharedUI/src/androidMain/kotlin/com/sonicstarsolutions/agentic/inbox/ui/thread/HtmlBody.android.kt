package com.sonicstarsolutions.agentic.inbox.ui.thread

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun HtmlBody(html: String, backgroundColor: Color, modifier: Modifier) {
    val backgroundArgb = backgroundColor.toArgb()
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                // Off, not on: the content already carries its own responsive viewport meta tag
                // and CSS (EmailHtmlDocumentBuilder) — with these on, WebView instead renders at
                // the page's "ideal" desktop width (most email HTML has a fixed ~600px table
                // layout) and zooms the whole thing out to fit, shrinking all the text.
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
                setBackgroundColor(backgroundArgb)
            }
        },
        modifier = modifier,
        update = { webView ->
            webView.setBackgroundColor(backgroundArgb)
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        },
    )
}
