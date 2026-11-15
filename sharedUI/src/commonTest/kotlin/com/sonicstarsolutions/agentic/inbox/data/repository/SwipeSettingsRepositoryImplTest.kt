package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.SwipeSettingsStorage
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeSettingsRepositoryImplTest {

    private class FakeSwipeSettingsStorage(var stored: SwipeSettings = SwipeSettings()) : SwipeSettingsStorage {
        val writes = mutableListOf<SwipeSettings>()

        override suspend fun read(): SwipeSettings = stored

        override suspend fun write(settings: SwipeSettings) {
            writes += settings
            stored = settings
        }
    }

    private fun TestScope.buildRepository(storage: FakeSwipeSettingsStorage) =
        SwipeSettingsRepositoryImpl(storage, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    @Test
    fun `nothing stored means archive on right and delete on left`() = runTest {
        val repository = buildRepository(FakeSwipeSettingsStorage())

        assertEquals(SwipeAction.Archive, repository.settings.value.swipeRight)
        assertEquals(SwipeAction.Delete, repository.settings.value.swipeLeft)
    }

    @Test
    fun `a new repository hydrates itself from storage`() = runTest {
        val stored = SwipeSettings(swipeRight = SwipeAction.ToggleRead, swipeLeft = SwipeAction.None)

        val repository = buildRepository(FakeSwipeSettingsStorage(stored))

        assertEquals(stored, repository.settings.value)
    }

    @Test
    fun `setSwipeRight persists and publishes without touching the left action`() = runTest {
        val storage = FakeSwipeSettingsStorage()
        val repository = buildRepository(storage)

        repository.setSwipeRight(SwipeAction.ToggleStar)

        val expected = SwipeSettings(swipeRight = SwipeAction.ToggleStar, swipeLeft = SwipeAction.Delete)
        assertEquals(expected, repository.settings.value)
        assertEquals(listOf(expected), storage.writes)
    }

    @Test
    fun `setSwipeLeft persists and publishes without touching the right action`() = runTest {
        val storage = FakeSwipeSettingsStorage()
        val repository = buildRepository(storage)

        repository.setSwipeLeft(SwipeAction.Archive)

        val expected = SwipeSettings(swipeRight = SwipeAction.Archive, swipeLeft = SwipeAction.Archive)
        assertEquals(expected, repository.settings.value)
        assertEquals(listOf(expected), storage.writes)
    }
}
