package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ValidateConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val baseUrl: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val validating: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class OnboardingViewModel(
    private val saveCredentials: SaveCredentialsUseCase,
    private val validateConnection: ValidateConnectionUseCase,
    private val observeCredentials: ObserveCredentialsUseCase,
    private val loadCredentials: LoadCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState().with(observeCredentials().value)
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        initCredentials()
    }

    fun initCredentials() {
        viewModelScope.launch { loadCredentials() }
        viewModelScope.launch {
            observeCredentials().collect { snapshot ->
                _state.update { current -> current.with(snapshot) }
            }
        }
    }

    fun onBaseUrlChanged(value: String) = _state.update { it.copy(baseUrl = value, errorMessage = null) }
    fun onClientIdChanged(value: String) = _state.update { it.copy(clientId = value, errorMessage = null) }
    fun onClientSecretChanged(value: String) = _state.update { it.copy(clientSecret = value, errorMessage = null) }

    fun validateAndSave() {
        val current = _state.value
        if (current.validating) return
        if (current.baseUrl.isBlank() || current.clientId.isBlank() || current.clientSecret.isBlank()) {
            _state.update { it.copy(errorMessage = "All fields are required.") }
            return
        }
        _state.update { it.copy(validating = true, errorMessage = null) }

        viewModelScope.launch {
            val credentials = Credentials(
                baseUrl = current.baseUrl.trim().trimEnd('/'),
                clientId = current.clientId.trim(),
                clientSecret = current.clientSecret.trim(),
            )
            saveCredentials(credentials)
            validateConnection()
                .onSuccess {
                    _state.update { it.copy(validating = false, saved = true) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            validating = false,
                            errorMessage = t.message ?: "Unknown error",
                        )
                    }
                }
        }
    }

    fun consumeSaved() = _state.update { it.copy(saved = false) }

    private fun OnboardingUiState.with(credentials: Credentials): OnboardingUiState =
        copy(
            baseUrl = credentials.baseUrl,
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret,
        )
}
