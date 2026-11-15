package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.SwipeSettingsStorage
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.SwipeSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SwipeSettingsRepositoryImpl(
    private val storage: SwipeSettingsStorage,
    scope: CoroutineScope,
) : SwipeSettingsRepository {

    private val _settings = MutableStateFlow(SwipeSettings())
    override val settings: StateFlow<SwipeSettings> = _settings.asStateFlow()

    init {
        // Self-hydrating, like CredentialsRepositoryImpl: nothing else is responsible for loading
        // this, and the inbox must show the user's choice from its first frame after a restart.
        // compareAndSet so a slow read can't overwrite a choice made while it was in flight.
        scope.launch {
            _settings.compareAndSet(SwipeSettings(), storage.read())
        }
    }

    override suspend fun setSwipeRight(action: SwipeAction) = change { it.copy(swipeRight = action) }

    override suspend fun setSwipeLeft(action: SwipeAction) = change { it.copy(swipeLeft = action) }

    private suspend fun change(transform: (SwipeSettings) -> SwipeSettings) {
        val updated = transform(_settings.value)
        _settings.update { updated }
        storage.write(updated)
    }
}
