package com.sonicstarsolutions.agentic.inbox.platform

import android.content.Context

/** Reads `versionName` from the installed package, which the app module sets in Gradle. */
class AndroidAppInfo(context: Context) : AppInfo {
    override val versionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: AppInfo.UNKNOWN_VERSION
}
