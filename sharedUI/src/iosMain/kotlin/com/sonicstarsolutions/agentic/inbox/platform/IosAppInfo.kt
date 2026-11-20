package com.sonicstarsolutions.agentic.inbox.platform

import platform.Foundation.NSBundle

/** Reads `CFBundleShortVersionString` from the app's Info.plist. */
class IosAppInfo : AppInfo {
    override val versionName: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: AppInfo.UNKNOWN_VERSION
}
