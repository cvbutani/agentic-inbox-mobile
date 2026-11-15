package com.sonicstarsolutions.agentic.inbox.ui.thread

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sonicstarsolutions.agentic.inbox.util.ExternalLinkPolicy
import kotlinx.coroutines.delay

@Composable
actual fun HtmlBody(html: String, backgroundColor: Color, modifier: Modifier) {
    val backgroundArgb = backgroundColor.toArgb()
    val density = LocalDensity.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var contentHeight by remember(html) { mutableStateOf<Dp?>(null) }

    // JavaScript is off (these are untrusted email bodies), so there's no JS bridge to report
    // document height back. WebView.contentHeight exposes it natively instead — but it only
    // becomes accurate some time after layout, and grows as images and fonts settle. Poll it
    // until it holds steady, then stop.
    LaunchedEffect(webView, html) {
        val view = webView ?: return@LaunchedEffect
        var stableFor = 0
        var lastHeight = 0
        repeat(MAX_MEASURE_ATTEMPTS) {
            delay(MEASURE_INTERVAL_MILLIS)
            // Before the view has real layout width, WebView reflows text into a single
            // near-zero-width column, reporting a contentHeight of hundreds of thousands of
            // px. Skip those readings rather than latching onto a bogus giant height.
            if (view.width <= 0) return@repeat
            val measured = (view.contentHeight * view.scale).toInt().coerceAtMost(MAX_HEIGHT_PX)
            if (measured > 0 && measured == lastHeight) {
                stableFor++
                if (stableFor >= STABLE_MEASUREMENTS_REQUIRED) {
                    contentHeight = with(density) { measured.toDp() }
                    return@LaunchedEffect
                }
            } else {
                stableFor = 0
                lastHeight = measured
                if (measured > 0) {
                    // Publish intermediate measurements too, so a tall message expands as it
                    // loads instead of sitting at the placeholder height until fully settled.
                    contentHeight = with(density) { measured.toDp() }
                }
            }
        }
    }

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
                webViewClient = ExternalLinkWebViewClient()
                // The view ends up exactly as tall as its content, so there is nothing left for it
                // to scroll internally — these just stop a scrollbar flashing while it settles.
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webView = this
            }
        },
        // Until the first measurement lands, hold a placeholder height — a zero-height WebView
        // never lays out, so it would never report a content height and would stay at zero.
        modifier = modifier.then(
            contentHeight?.let { Modifier.height(it) } ?: Modifier.heightIn(min = PLACEHOLDER_HEIGHT),
        ),
        update = { view ->
            view.setBackgroundColor(backgroundArgb)
            // Reload only when the content actually changed. Measuring the height feeds a state
            // change back into this composable, so an unconditional load here would restart the
            // page load on every measurement — which changes the height again, and so on.
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
    )
}

/**
 * Sends tapped links to the user's default handler (browser, mail or dialer app) instead of
 * navigating the email body view away from the message. Returning true always keeps the view on
 * the message; schemes outside [ExternalLinkPolicy] are dropped rather than followed.
 */
private class ExternalLinkWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (ExternalLinkPolicy.opensExternally(url)) {
            try {
                view.context.startActivity(
                    Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
                // No app can handle this link (e.g. a tel: link on a tablet) — nothing to open.
            }
        }
        return true
    }
}

private val PLACEHOLDER_HEIGHT = 120.dp
private const val MEASURE_INTERVAL_MILLIS = 50L
private const val MAX_MEASURE_ATTEMPTS = 60
private const val STABLE_MEASUREMENTS_REQUIRED = 3

// A generous ceiling for a rendered email body — no legitimate message needs a taller WebView,
// and this keeps any future bogus measurement from exceeding what Compose's Constraints can hold.
private const val MAX_HEIGHT_PX = 20_000
