package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.StageCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ValidateConnectionUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeConnectionRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        credentialsRepository: FakeCredentialsRepository = FakeCredentialsRepository(),
        connectionRepository: FakeConnectionRepository = FakeConnectionRepository(),
    ): OnboardingViewModel = OnboardingViewModel(
        saveCredentials = SaveCredentialsUseCase(credentialsRepository),
        stageCredentials = StageCredentialsUseCase(credentialsRepository),
        validateConnection = ValidateConnectionUseCase(connectionRepository),
        observeCredentials = ObserveCredentialsUseCase(credentialsRepository),
        loadCredentials = LoadCredentialsUseCase(credentialsRepository),
    )

    @Test
    fun `validateAndSave rejects blank fields without touching the repositories`() = runTest {
        val credentialsRepository = FakeCredentialsRepository()
        val viewModel = buildViewModel(credentialsRepository)

        viewModel.onBaseUrlChanged("my-worker.example.dev")
        // clientId/clientSecret left blank
        viewModel.validateAndSave()

        assertEquals("All fields are required.", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.saved)
        assertEquals(Credentials(), credentialsRepository.stored)
    }

    @Test
    fun `validateAndSave trims fields saves and marks saved on a successful connection`() = runTest {
        val credentialsRepository = FakeCredentialsRepository()
        val viewModel = buildViewModel(
            credentialsRepository,
            FakeConnectionRepository(result = Result.success(Unit)),
        )

        viewModel.onBaseUrlChanged("  my-worker.example.dev/  ")
        viewModel.onClientIdChanged(" id ")
        viewModel.onClientSecretChanged(" secret ")
        viewModel.validateAndSave()

        assertEquals(
            Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "secret"),
            credentialsRepository.stored,
        )
        assertTrue(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.validating)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `validateAndSave surfaces the connection failure and does not mark saved`() = runTest {
        val viewModel = buildViewModel(
            connectionRepository = FakeConnectionRepository(result = Result.failure(RuntimeException("unreachable"))),
        )

        viewModel.onBaseUrlChanged("my-worker.example.dev")
        viewModel.onClientIdChanged("id")
        viewModel.onClientSecretChanged("secret")
        viewModel.validateAndSave()

        assertEquals("unreachable", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.validating)
    }

    @Test
    fun `a failed connection never persists the credentials that were tried`() = runTest {
        // Regression: SplashViewModel decides where the app opens purely from whether *stored*
        // credentials are complete (see SplashViewModelTest). If a failed validate here left bad
        // credentials in storage, the next cold launch would read them back as "complete" and
        // route straight past Onboarding into the mailbox picker — the exact bug this guards.
        val credentialsRepository = FakeCredentialsRepository()
        val viewModel = buildViewModel(
            credentialsRepository = credentialsRepository,
            connectionRepository = FakeConnectionRepository(result = Result.failure(RuntimeException("unreachable"))),
        )

        viewModel.onBaseUrlChanged("bad-worker.example.dev")
        viewModel.onClientIdChanged("id")
        viewModel.onClientSecretChanged("secret")
        viewModel.validateAndSave()

        assertEquals(Credentials(), credentialsRepository.stored, "nothing should ever reach durable storage")
    }

    @Test
    fun `validateAndSave stages the candidate credentials before validating so the connection attempt uses them`() = runTest {
        val credentialsRepository = FakeCredentialsRepository()
        val viewModel = buildViewModel(
            credentialsRepository = credentialsRepository,
            connectionRepository = FakeConnectionRepository(result = Result.failure(RuntimeException("unreachable"))),
        )

        viewModel.onBaseUrlChanged("my-worker.example.dev")
        viewModel.onClientIdChanged("id")
        viewModel.onClientSecretChanged("secret")
        viewModel.validateAndSave()

        assertEquals(
            Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "secret"),
            credentialsRepository.stageCalls.single(),
        )
    }

    @Test
    fun `consumeSaved clears the saved flag`() = runTest {
        val viewModel = buildViewModel(
            connectionRepository = FakeConnectionRepository(result = Result.success(Unit)),
        )
        viewModel.onBaseUrlChanged("my-worker.example.dev")
        viewModel.onClientIdChanged("id")
        viewModel.onClientSecretChanged("secret")
        viewModel.validateAndSave()
        assertTrue(viewModel.state.value.saved)

        viewModel.consumeSaved()

        assertFalse(viewModel.state.value.saved)
    }

    @Test
    fun `field changes clear any existing error message`() = runTest {
        val viewModel = buildViewModel()
        viewModel.validateAndSave() // sets "All fields are required."
        assertEquals("All fields are required.", viewModel.state.value.errorMessage)

        viewModel.onBaseUrlChanged("my-worker.example.dev")

        assertNull(viewModel.state.value.errorMessage)
    }
}
