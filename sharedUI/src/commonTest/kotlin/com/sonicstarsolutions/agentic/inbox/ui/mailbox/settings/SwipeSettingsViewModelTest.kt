package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeAction
import com.sonicstarsolutions.agentic.inbox.domain.model.SwipeSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.SwipeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeSettingsViewModelTest {

    private class FakeSwipeSettingsRepository : SwipeSettingsRepository {
        override val settings = MutableStateFlow(SwipeSettings())
        override suspend fun setSwipeRight(action: SwipeAction) {
            settings.value = settings.value.copy(swipeRight = action)
        }

        override suspend fun setSwipeLeft(action: SwipeAction) {
            settings.value = settings.value.copy(swipeLeft = action)
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes the stored settings`() = runTest {
        val repository = FakeSwipeSettingsRepository()
        repository.settings.value = SwipeSettings(swipeRight = SwipeAction.None, swipeLeft = SwipeAction.ToggleRead)

        val viewModel = SwipeSettingsViewModel(repository)

        assertEquals(SwipeAction.None, viewModel.settings.value.swipeRight)
        assertEquals(SwipeAction.ToggleRead, viewModel.settings.value.swipeLeft)
    }

    @Test
    fun `choosing a right action updates the settings immediately`() = runTest {
        val viewModel = SwipeSettingsViewModel(FakeSwipeSettingsRepository())

        viewModel.onSwipeRightChanged(SwipeAction.ToggleStar)

        assertEquals(SwipeAction.ToggleStar, viewModel.settings.value.swipeRight)
        assertEquals(SwipeAction.Delete, viewModel.settings.value.swipeLeft)
    }

    @Test
    fun `choosing a left action updates the settings immediately`() = runTest {
        val viewModel = SwipeSettingsViewModel(FakeSwipeSettingsRepository())

        viewModel.onSwipeLeftChanged(SwipeAction.Archive)

        assertEquals(SwipeAction.Archive, viewModel.settings.value.swipeLeft)
        assertEquals(SwipeAction.Archive, viewModel.settings.value.swipeRight)
    }

    @Test
    fun `the options offered are every action once`() {
        assertEquals(
            listOf(SwipeAction.Archive, SwipeAction.Delete, SwipeAction.ToggleRead, SwipeAction.ToggleStar, SwipeAction.None),
            SwipeAction.entries,
        )
    }
}
