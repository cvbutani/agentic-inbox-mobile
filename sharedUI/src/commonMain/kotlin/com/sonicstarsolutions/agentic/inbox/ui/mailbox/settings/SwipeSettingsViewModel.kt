package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.SwipeSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Backs the "Swipe actions" section of the settings screen. Unlike the mailbox fields beside it,
 * these are device preferences with no Save step: a choice applies — and is stored — as soon as
 * it is made.
 */
class SwipeSettingsViewModel(
    private val repository: SwipeSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<SwipeSettings> = repository.settings

    fun onSwipeRightChanged(action: SwipeAction) {
        viewModelScope.launch { repository.setSwipeRight(action) }
    }

    fun onSwipeLeftChanged(action: SwipeAction) {
        viewModelScope.launch { repository.setSwipeLeft(action) }
    }
}
