package com.sonicstarsolutions.agentic.inbox.ui.inbox

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun summary(id: String) = EmailSummary(
        id = id,
        subject = "Subject $id",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        date = "2026-07-16T00:00:00Z",
        read = false,
        starred = false,
        threadId = null,
        folderId = "inbox",
        snippet = null,
    )

    private fun buildViewModel(
        repository: FakeEmailRepository,
        folderRepository: FakeFolderRepository = FakeFolderRepository(),
    ): InboxViewModel = InboxViewModel(
        getEmails = GetEmailsUseCase(repository),
        getFolders = GetFoldersUseCase(folderRepository),
        mailboxId = "mb1",
        mailboxName = "Inbox",
    )

    @Test
    fun `loadFirstPage populates emails and totalCount on success`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.emails)
        assertEquals(2, viewModel.state.value.totalCount)
        assertEquals(1, viewModel.state.value.page)
    }

    @Test
    fun `loadFirstPage surfaces the failure message`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.failure(RuntimeException("server down")) },
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals("server down", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.emails.isEmpty())
    }

    @Test
    fun `loadMore appends the next page and advances the page counter`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, page, _ ->
                val item = summary("e$page")
                Result.success(EmailPage(listOf(item), totalCount = 3))
            },
        )
        val viewModel = buildViewModel(repository) // page 1 -> [e1], totalCount 3

        viewModel.loadMore()

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.emails)
        assertEquals(2, viewModel.state.value.page)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `loadMore does nothing once every email has been loaded`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository) // already fully loaded: 1 of 1

        viewModel.loadMore()

        assertEquals(1, repository.calls.size, "loadMore should not have issued a second request")
        assertEquals(1, viewModel.state.value.page)
    }

    @Test
    fun `onRefresh replaces the list and resets to page one`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        repository.handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("fresh")), totalCount = 1)) }

        viewModel.onRefresh()

        assertEquals(listOf(summary("fresh")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.page)
        assertFalse(viewModel.state.value.refreshing)
    }

    @Test
    fun `onRefresh ignores a second call while one is already in flight`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        viewModel.onRefresh()
        assertTrue(viewModel.state.value.refreshing)
        val callsWhileInFlight = repository.calls.size

        viewModel.onRefresh() // should be ignored by the `refreshing` guard

        assertEquals(callsWhileInFlight, repository.calls.size, "second onRefresh should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.refreshing)
    }

    @Test
    fun `the view model starts on the Inbox folder before folders finish loading`() = runTest {
        val emailRepository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(emptyList(), totalCount = 0)) },
        )

        val viewModel = buildViewModel(emailRepository)

        assertEquals(SystemFolders.INBOX, viewModel.state.value.currentFolder.id)
    }

    @Test
    fun `folders load into state on init`() = runTest {
        val customFolder = Folder(id = "work", name = "Work", unreadCount = 4, isSystem = false)
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + customFolder))
        val emailRepository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(emptyList(), totalCount = 0)) },
        )

        val viewModel = buildViewModel(emailRepository, folderRepository)

        assertEquals(SystemFolders.defaults + customFolder, viewModel.state.value.folders)
    }

    @Test
    fun `selectFolder switches folders and reloads page one for the new folder`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, folder, _, _ -> Result.success(EmailPage(listOf(summary(folder)), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        val draftsFolder = SystemFolders.defaults.first { it.id == SystemFolders.DRAFT }

        viewModel.selectFolder(draftsFolder)

        assertEquals(draftsFolder, viewModel.state.value.currentFolder)
        assertEquals(listOf(summary("draft")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.page)
        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf("inbox", "draft"), repository.calls.map { it.folder })
    }

    @Test
    fun `selectFolder is a no-op when selecting the folder already active`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        val inboxFolder = SystemFolders.defaults.first { it.id == SystemFolders.INBOX }

        viewModel.selectFolder(inboxFolder)

        assertEquals(1, repository.calls.size, "reselecting the active folder should not issue a new request")
    }
}
