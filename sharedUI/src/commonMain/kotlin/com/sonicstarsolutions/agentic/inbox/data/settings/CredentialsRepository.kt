package com.sonicstarsolutions.agentic.inbox.data.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads/writes [Credentials] to a multiplatform-settings-backed store.
 * M0 keeps values in plain Settings; encryption is a v0.1 follow-up.
 */
class CredentialsRepository(
    private val settings: Settings,
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Credentials> = _state.asStateFlow()

    fun save(credentials: Credentials) {
        settings.putString(KEY_BASE_URL, credentials.baseUrl)
        settings.putString(KEY_CLIENT_ID, credentials.clientId)
        settings.putString(KEY_CLIENT_SECRET, credentials.clientSecret)
        _state.value = credentials
    }

    fun clear() {
        settings.remove(KEY_BASE_URL)
        settings.remove(KEY_CLIENT_ID)
        settings.remove(KEY_CLIENT_SECRET)
        _state.value = Credentials("", "", "")
    }

    fun current(): Credentials = _state.value

    private fun read(): Credentials = Credentials(
        baseUrl = settings.getString(KEY_BASE_URL, ""),
        clientId = settings.getString(KEY_CLIENT_ID, ""),
        clientSecret = settings.getString(KEY_CLIENT_SECRET, ""),
    )

    private companion object {
        const val KEY_BASE_URL = "baseUrl"
        const val KEY_CLIENT_ID = "cfAccessClientId"
        const val KEY_CLIENT_SECRET = "cfAccessClientSecret"
    }
}
