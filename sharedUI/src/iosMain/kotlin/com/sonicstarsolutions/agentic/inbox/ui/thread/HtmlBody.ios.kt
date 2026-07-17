package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
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
    var webView by remember { mutableStateOf<WKWebView?>(null) }
    var contentHeight by remember(html) { mutableStateOf<Dp?>(null) }
    // Measuring the height feeds a state change back into this composable, so `update` must not
    // reload unconditionally — that would restart the load on every measurement, which changes
    // the height again, and so on.
    val loadedHtml = remember { mutableStateOf<String?>(null) }

    // JavaScript is off (these are untrusted email bodies), so document.body.scrollHeight isn't
    // reachable. The scroll view's own contentSize is the same number by another route — poll it
    // until it holds steady, since it grows as images and fonts settle. Points map 1:1 to dp.
    LaunchedEffect(webView, html) {
        val view = webView ?: return@LaunchedEffect
        var stableFor = 0
        var lastHeight = 0.0
        repeat(MAX_MEASURE_ATTEMPTS) {
            delay(MEASURE_INTERVAL_MILLIS)
            val measured = view.scrollView.contentSize.useContents { height }
            if (measured > 0.0 && measured == lastHeight) {
                stableFor++
                if (stableFor >= STABLE_MEASUREMENTS_REQUIRED) {
                    contentHeight = measured.dp
                    return@LaunchedEffect
                }
            } else {
                stableFor = 0
                lastHeight = measured
                if (measured > 0.0) {
                    // Publish intermediate measurements too, so a tall message expands as it
                    // loads instead of sitting at the placeholder height until fully settled.
                    contentHeight = measured.dp
                }
            }
        }
    }

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
                // The view ends up exactly as tall as its content and scrolls with the thread's
                // LazyColumn; leaving its own scrolling on would swallow those drags.
                this.scrollView.scrollEnabled = false
                this.scrollView.bounces = false
                webView = this
            }
        },
        // Until the first measurement lands, hold a placeholder height — a zero-height web view
        // never lays out, so it would never report a content size and would stay at zero.
        modifier = modifier.then(
            contentHeight?.let { Modifier.height(it) } ?: Modifier.heightIn(min = PLACEHOLDER_HEIGHT),
        ),
        update = { view ->
            view.backgroundColor = uiColor
            view.scrollView.backgroundColor = uiColor
            if (loadedHtml.value != html) {
                loadedHtml.value = html
                view.loadHTMLString(html, baseURL = null)
            }
        },
    )
}

private val PLACEHOLDER_HEIGHT = 120.dp
private const val MEASURE_INTERVAL_MILLIS = 50L
private const val MAX_MEASURE_ATTEMPTS = 60
private const val STABLE_MEASUREMENTS_REQUIRED = 3
