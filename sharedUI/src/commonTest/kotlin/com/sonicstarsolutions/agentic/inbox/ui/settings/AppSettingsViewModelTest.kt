package com.sonicstarsolutions.agentic.inbox.ui.settings

import com.sonicstarsolutions.agentic.inbox.domain.model.BackendInfo
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearLocalCacheUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SignOutUseCase
import com.sonicstarsolutions.agentic.inbox.platform.AppInfo
import com.sonicstarsolutions.agentic.inbox.testutil.FakeCredentialsRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
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
class AppSettingsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAppInfo(override val versionName: String = "1.2.0") : AppInfo

    private val credentials = Credentials(
        baseUrl = "https://mail.example.dev/",
        clientId = "client-id-value",
        clientSecret = "client-secret-value",
    )

    private fun buildViewModel(
        credentialsRepository: FakeCredentialsRepository = FakeCredentialsRepository(),
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository(),
        folderRepository: FakeFolderRepository = FakeFolderRepository(),
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        appInfo: AppInfo = FakeAppInfo(),
    ) = AppSettingsViewModel(
        observeCredentials = ObserveCredentialsUseCase(credentialsRepository),
        signOut = SignOutUseCase(
            clearCredentials = ClearCredentialsUseCase(credentialsRepository),
            clearLocalCache = ClearLocalCacheUseCase(mailboxRepository, folderRepository, emailRepository),
        ),
        appInfo = appInfo,
    )

    @Test
    fun `shows the connected host without the scheme`() = runTest {
        val repository = FakeCredentialsRepository()
        repository.save(credentials)

        val state = buildViewModel(repository).state.value

        assertEquals("mail.example.dev", state.connectedHost)
    }

    @Test
    fun `never exposes the client id or secret`() = runTest {
        val repository = FakeCredentialsRepository()
        repository.save(credentials)

        val state = buildViewModel(repository).state.value

        assertFalse("client-secret-value" in state.toString())
        assertFalse("client-id-value" in state.toString())
    }

    @Test
    fun `no connected host when nothing is saved`() = runTest {
        assertNull(buildViewModel().state.value.connectedHost)
    }

    @Test
    fun `carries the backend name and link from BackendInfo`() = runTest {
        val state = buildViewModel().state.value

        assertEquals(BackendInfo.NAME, state.backendName)
        assertEquals(BackendInfo.REPO_URL, state.repoUrl)
    }

    @Test
    fun `reports the app version`() = runTest {
        assertEquals("3.4.5", buildViewModel(appInfo = FakeAppInfo("3.4.5")).state.value.versionName)
    }

    @Test
    fun `signOut clears credentials and the local cache then flags signedOut`() = runTest {
        val repository = FakeCredentialsRepository()
        repository.save(credentials)
        val mailboxRepository = FakeMailboxRepository()
        val viewModel = buildViewModel(repository, mailboxRepository)

        viewModel.signOut()

        assertEquals(Credentials(), repository.stored)
        assertTrue(mailboxRepository.clearCacheCalled)
        assertTrue(viewModel.state.value.signedOut)
    }

    @Test
    fun `consumeSignedOut clears the flag`() = runTest {
        val viewModel = buildViewModel()
        viewModel.signOut()

        viewModel.consumeSignedOut()

        assertFalse(viewModel.state.value.signedOut)
    }
}
