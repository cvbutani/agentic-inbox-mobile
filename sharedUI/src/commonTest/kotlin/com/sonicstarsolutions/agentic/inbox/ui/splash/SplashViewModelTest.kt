package com.sonicstarsolutions.agentic.inbox.ui.splash

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeCredentialsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(credentialsRepository: FakeCredentialsRepository): SplashViewModel =
        SplashViewModel(
            loadCredentials = LoadCredentialsUseCase(credentialsRepository),
            observeCredentials = ObserveCredentialsUseCase(credentialsRepository),
        )

    @Test
    fun `signedIn is true when stored credentials are complete`() = runTest {
        val repository = FakeCredentialsRepository(
            stored = Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "secret"),
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.signedIn)
    }

    @Test
    fun `signedIn is false when no credentials are stored`() = runTest {
        val repository = FakeCredentialsRepository(stored = Credentials())

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertFalse(viewModel.state.value.signedIn)
    }

    @Test
    fun `signedIn is false when credentials are only partially filled in`() = runTest {
        val repository = FakeCredentialsRepository(
            stored = Credentials(baseUrl = "my-worker.example.dev"),
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.signedIn)
    }
}
