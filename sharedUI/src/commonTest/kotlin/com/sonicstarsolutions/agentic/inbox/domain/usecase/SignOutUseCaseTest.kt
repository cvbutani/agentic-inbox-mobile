package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.testutil.FakeCredentialsRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignOutUseCaseTest {

    private val credentialsRepository = FakeCredentialsRepository(
        stored = Credentials(baseUrl = "https://mail.example.dev", clientId = "id", clientSecret = "secret"),
    )
    private val mailboxRepository = FakeMailboxRepository()
    private val folderRepository = FakeFolderRepository()
    private val emailRepository = FakeEmailRepository()

    private val signOut = SignOutUseCase(
        clearCredentials = ClearCredentialsUseCase(credentialsRepository),
        clearLocalCache = ClearLocalCacheUseCase(mailboxRepository, folderRepository, emailRepository),
    )

    @Test
    fun `clears the stored credentials`() = runTest {
        signOut()

        assertEquals(Credentials(), credentialsRepository.stored)
    }

    @Test
    fun `also clears the local mailbox folder and email cache`() = runTest {
        // Otherwise a different account signing in on this device would read the previous
        // account's mail straight out of the offline cache before its first refresh completes.
        signOut()

        assertTrue(mailboxRepository.clearCacheCalled)
        assertTrue(folderRepository.clearCacheCalled)
        assertTrue(emailRepository.clearCacheCalled)
    }
}
