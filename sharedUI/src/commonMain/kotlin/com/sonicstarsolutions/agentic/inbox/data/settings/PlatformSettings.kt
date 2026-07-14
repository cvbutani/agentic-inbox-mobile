package com.sonicstarsolutions.agentic.inbox.data.settings

import com.russhwolf.settings.Settings

/**
 * Platform-default Settings factory. Uses multiplatform-settings-no-arg so
 * each host provides a default container without context plumbing. Switch
 * to EncryptedSharedPreferences / Keychain in a follow-up — see plan §3.
 */
expect fun platformSettings(name: String = "agentic_inbox_prefs"): Settings
