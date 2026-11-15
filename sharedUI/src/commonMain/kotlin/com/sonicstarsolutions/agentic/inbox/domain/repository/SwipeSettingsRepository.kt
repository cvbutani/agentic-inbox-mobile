package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import kotlinx.coroutines.flow.StateFlow

interface SwipeSettingsRepository {
    val settings: StateFlow<SwipeSettings>

    suspend fun setSwipeRight(action: SwipeAction)
    suspend fun setSwipeLeft(action: SwipeAction)
}
