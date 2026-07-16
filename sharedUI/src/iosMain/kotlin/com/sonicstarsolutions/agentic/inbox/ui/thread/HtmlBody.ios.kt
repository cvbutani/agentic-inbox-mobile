package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HtmlBody(html: String, backgroundColor: Color, modifier: Modifier) {
    val uiColor = UIColor(
        red = backgroundColor.red.toDouble(),
        green = backgroundColor.green.toDouble(),
        blue = backgroundColor.blue.toDouble(),
        alpha = backgroundColor.alpha.toDouble(),
    )
    UIKitView(
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                defaultWebpagePreferences.allowsContentJavaScript = false
            }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
                opaque = false
                // Explicit `this.` — a bare `backgroundColor =` here resolves to the outer
                // HtmlBody(backgroundColor: Color) parameter instead of WKWebView's own property.
                this.backgroundColor = uiColor
                this.scrollView.backgroundColor = uiColor
            }
        },
        modifier = modifier,
        update = { webView ->
            webView.backgroundColor = uiColor
            webView.scrollView.backgroundColor = uiColor
            webView.loadHTMLString(html, baseURL = null)
        },
    )
}
