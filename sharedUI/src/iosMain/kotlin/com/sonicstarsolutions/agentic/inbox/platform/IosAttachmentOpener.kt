package com.sonicstarsolutions.agentic.inbox.platform

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Both actions present the system share sheet: it carries Quick Look, "Open in", and share
 * targets in one surface, and avoids UIDocumentInteractionController's delegate-lifetime
 * pitfalls entirely.
 */
class IosAttachmentOpener : AttachmentOpener {

    override fun open(path: String, mimeType: String): Result<Unit> = presentShareSheet(path)

    override fun share(path: String, mimeType: String): Result<Unit> = presentShareSheet(path)

    private fun presentShareSheet(path: String): Result<Unit> {
        val url = NSURL.fileURLWithPath(path)
        dispatch_async(dispatch_get_main_queue()) {
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@dispatch_async
            val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
            // iPad requires a popover anchor; the root view keeps it centred enough.
            controller.popoverPresentationController?.sourceView = root.view
            var top = root
            while (top.presentedViewController != null) {
                top = top.presentedViewController!!
            }
            top.presentViewController(controller, animated = true, completion = null)
        }
        return Result.success(Unit)
    }
}
