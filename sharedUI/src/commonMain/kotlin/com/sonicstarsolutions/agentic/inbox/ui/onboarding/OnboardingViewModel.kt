package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.settings.Credentials
import com.sonicstarsolutions.agentic.inbox.data.settings.CredentialsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText

data class OnboardingUiState(
    val baseUrl: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val validating: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class OnboardingViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val api: AgenticInboxApi,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState().with(credentialsRepository.current())
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

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
            credentialsRepository.save(credentials)
            runCatching { api.getConfig() }
                .onSuccess {
                    _state.update { it.copy(validating = false, saved = true) }
                }
                .onFailure { t ->
                    val message = describeError(t)
                    _state.update {
                        it.copy(
                            validating = false,
                            errorMessage = message,
                        )
                    }
                }
        }
    }

    fun consumeSaved() = _state.update { it.copy(saved = false) }

    private suspend fun describeError(t: Throwable): String = when (t) {
        is ResponseException -> {
            val code = t.response.status.value
            val bodyText = runCatching { t.response.bodyAsText() }.getOrDefault("")
            val body = bodyText.take(512).trim()
            if (body.isNotEmpty()) "Server returned HTTP $code\n\n$body"
            else "Server returned HTTP $code"
        }
        else -> t.message ?: t::class.simpleName ?: "Unknown error"
    }

    private fun OnboardingUiState.with(credentials: Credentials): OnboardingUiState =
        copy(
            baseUrl = credentials.baseUrl,
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret,
        )
}
