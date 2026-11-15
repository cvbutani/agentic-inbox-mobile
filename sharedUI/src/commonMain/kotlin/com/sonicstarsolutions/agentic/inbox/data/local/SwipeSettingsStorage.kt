package com.sonicstarsolutions.agentic.inbox.data.local

import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import eu.anifantakis.lib.ksafe.KSafe

/** The persistence seam for [SwipeSettings] — KSafe in production, an in-memory fake in tests. */
interface SwipeSettingsStorage {
    suspend fun read(): SwipeSettings
    suspend fun write(settings: SwipeSettings)
}

class KSafeSwipeSettingsStorage(private val ksafe: KSafe) : SwipeSettingsStorage {
    companion object {
        private const val KEY_SWIPE_SETTINGS = "swipe_settings"
    }

    override suspend fun read(): SwipeSettings = ksafe.get(KEY_SWIPE_SETTINGS, SwipeSettings())

    override suspend fun write(settings: SwipeSettings) = ksafe.put(KEY_SWIPE_SETTINGS, settings)
}
