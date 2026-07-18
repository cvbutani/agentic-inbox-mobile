package com.sonicstarsolutions.agentic.inbox.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Opens/shares cached attachment files through content URIs granted via the app's
 * FileProvider (declared in androidApp's manifest with the `.fileprovider` authority). */
class AndroidAttachmentOpener(context: Context) : AttachmentOpener {

    private val appContext = context.applicationContext

    private fun contentUri(path: String) = FileProvider.getUriForFile(
        appContext,
        "${appContext.packageName}.fileprovider",
        File(path),
    )

    override fun open(path: String, mimeType: String): Result<Unit> = try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri(path), mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
        Result.success(Unit)
    } catch (e: ActivityNotFoundException) {
        Result.failure(IllegalStateException("No app installed can open this file type"))
    } catch (t: Throwable) {
        Result.failure(t)
    }

    override fun share(path: String, mimeType: String): Result<Unit> = try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri(path))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(chooser)
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
