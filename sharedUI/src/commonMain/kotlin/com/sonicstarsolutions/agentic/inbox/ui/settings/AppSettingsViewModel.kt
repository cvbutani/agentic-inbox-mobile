package com.sonicstarsolutions.agentic.inbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.BackendInfo
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SignOutUseCase
import com.sonicstarsolutions.agentic.inbox.platform.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val backendName: String = BackendInfo.NAME,
    val repoUrl: String = BackendInfo.REPO_URL,
    /** Host (and port) of the connected server; null when nothing is connected. Never a secret. */
    val connectedHost: String? = null,
    val versionName: String = AppInfo.UNKNOWN_VERSION,
    val signedOut: Boolean = false,
)

/**
 * Backs the app-level Settings screen: which backend this app needs and which server it is
 * connected to, the app version, and signing out. Device preferences such as swipe actions have
 * their own view model ([com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings.SwipeSettingsViewModel]).
 */
class AppSettingsViewModel(
    private val observeCredentials: ObserveCredentialsUseCase,
    private val signOut: SignOutUseCase,
    appInfo: AppInfo,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppSettingsUiState(
            connectedHost = observeCredentials().value.hostOrNull(),
            versionName = appInfo.versionName,
        ),
    )
    val state: StateFlow<AppSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeCredentials().collect { credentials ->
                _state.update { it.copy(connectedHost = credentials.hostOrNull()) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOut.invoke()
            _state.update { it.copy(signedOut = true) }
        }
    }

    fun consumeSignedOut() = _state.update { it.copy(signedOut = false) }

    private fun Credentials.hostOrNull(): String? = displayHost().ifBlank { null }
}
