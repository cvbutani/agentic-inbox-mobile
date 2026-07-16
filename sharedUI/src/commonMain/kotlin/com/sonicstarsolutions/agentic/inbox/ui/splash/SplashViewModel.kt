package com.sonicstarsolutions.agentic.inbox.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplashUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
)

/**
 * Owns the one-time "where does the app open" decision. Hydrates saved credentials from storage
 * and reports whether they're complete; the screen maps that to a navigation destination so this
 * ViewModel stays free of nav-layer types.
 */
class SplashViewModel(
    private val loadCredentials: LoadCredentialsUseCase,
    private val observeCredentials: ObserveCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadCredentials()
            _state.update { it.copy(loading = false, signedIn = observeCredentials().value.isComplete()) }
        }
    }
}
