package com.sonicstarsolutions.agentic.inbox.util

/**
 * Wraps a sanitized email body in a full HTML document tuned for the app's WebView:
 * - A mobile viewport meta tag, since most email HTML has a fixed ~600px table layout and the
 *   WebView otherwise renders it at that width and shrinks the whole page to fit, making all text
 *   tiny.
 * - An explicit, readable font-size/line-height (email HTML often sets none at all, or something
 *   tiny for desktop clients).
 * - Background/text/link colors driven by the app's current Material theme, so the message
 *   doesn't sit in a plain white box regardless of light/dark mode.
 */
object EmailHtmlDocumentBuilder {

    fun wrap(
        bodyHtml: String,
        textColorHex: String,
        backgroundColorHex: String,
        linkColorHex: String,
    ): String = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
        <style>
          :root { color-scheme: light dark; }
          html, body {
            margin: 0;
            padding: 8px;
            background-color: $backgroundColorHex;
            color: $textColorHex;
            font-family: -apple-system, Roboto, sans-serif;
            font-size: 16px;
            line-height: 1.5;
            word-wrap: break-word;
            overflow-wrap: break-word;
          }
          img, table { max-width: 100% !important; height: auto !important; }
          table { width: auto !important; }
          a { color: $linkColorHex; }
        </style>
        </head>
        <body>
        $bodyHtml
        </body>
        </html>
    """.trimIndent()
}
