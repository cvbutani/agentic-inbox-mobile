package com.sonicstarsolutions.agentic.inbox.platform

/**
 * Facts about the installed app that only the platform knows. There is no shared version source:
 * Android sets `versionName` in its Gradle config and iOS in its Info.plist, so each platform
 * reads its own.
 */
interface AppInfo {
    /** The user-visible version, e.g. "1.2.0". Never blank: falls back to [UNKNOWN_VERSION]. */
    val versionName: String

    companion object {
        const val UNKNOWN_VERSION = "dev"
    }
}
