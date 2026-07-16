package com.sonicstarsolutions.agentic.inbox.ui.inbox

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RenameFolderUseCase
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
        createFolderUseCase = CreateFolderUseCase(folderRepository),
        renameFolderUseCase = RenameFolderUseCase(folderRepository),
        deleteFolderUseCase = DeleteFolderUseCase(folderRepository),
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

    @Test
    fun `createFolder trims the name appends the new folder and signals folderCreated`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(
            createResult = { name -> Result.success(Folder(id = "work", name = name, isSystem = false)) },
        )
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.createFolder("  Work  ")

        assertEquals(listOf("mb1" to "Work"), folderRepository.createCalls)
        assertTrue(viewModel.state.value.folders.contains(Folder(id = "work", name = "Work", isSystem = false)))
        assertTrue(viewModel.state.value.folderCreated)
        assertFalse(viewModel.state.value.creatingFolder)
    }

    @Test
    fun `createFolder rejects a blank name without calling the repository`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.createFolder("   ")

        assertTrue(folderRepository.createCalls.isEmpty())
        assertEquals("Folder name is required.", viewModel.state.value.folderActionError)
    }

    @Test
    fun `createFolder surfaces the failure and does not mark folderCreated`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(createResult = { Result.failure(RuntimeException("name taken")) })
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.createFolder("Work")

        assertEquals("name taken", viewModel.state.value.folderActionError)
        assertFalse(viewModel.state.value.folderCreated)
        assertFalse(viewModel.state.value.creatingFolder)
    }

    @Test
    fun `createFolder ignores a second call while one is already in flight`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)
        val gate = CompletableDeferred<Unit>()
        folderRepository.createGate = gate

        viewModel.createFolder("Work")
        assertTrue(viewModel.state.value.creatingFolder)

        viewModel.createFolder("Personal") // should be ignored by the creatingFolder guard

        assertEquals(1, folderRepository.createCalls.size, "second createFolder should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.creatingFolder)
    }

    @Test
    fun `consumeFolderCreated clears the flag`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)
        viewModel.createFolder("Work")
        assertTrue(viewModel.state.value.folderCreated)

        viewModel.consumeFolderCreated()

        assertFalse(viewModel.state.value.folderCreated)
    }

    private val workFolder = Folder(id = "work", name = "Work", unreadCount = 2, isSystem = false)

    @Test
    fun `renameFolder trims the name updates the folder in state and signals folderRenamed`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.renameFolder(workFolder, "  Projects  ")

        assertEquals(listOf(Triple("mb1", "work", "Projects")), folderRepository.renameCalls)
        assertTrue(viewModel.state.value.folders.contains(Folder(id = "work", name = "Projects", unreadCount = 2, isSystem = false)))
        assertTrue(viewModel.state.value.folderRenamed)
        assertFalse(viewModel.state.value.renamingFolder)
    }

    @Test
    fun `renameFolder updates currentFolder when the renamed folder is selected`() = runTest {
        val emailRepository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(emptyList(), totalCount = 0)) },
        )
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        viewModel.selectFolder(workFolder)

        viewModel.renameFolder(workFolder, "Projects")

        assertEquals("Projects", viewModel.state.value.currentFolder.name)
    }

    @Test
    fun `renameFolder rejects a blank name without calling the repository`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.renameFolder(workFolder, "   ")

        assertTrue(folderRepository.renameCalls.isEmpty())
        assertEquals("Folder name is required.", viewModel.state.value.folderActionError)
    }

    @Test
    fun `renameFolder rejects renaming a system folder without calling the repository`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)
        val inboxFolder = SystemFolders.defaults.first { it.id == SystemFolders.INBOX }

        viewModel.renameFolder(inboxFolder, "My Inbox")

        assertTrue(folderRepository.renameCalls.isEmpty())
        assertEquals("System folders can't be renamed.", viewModel.state.value.folderActionError)
    }

    @Test
    fun `renameFolder surfaces the failure and does not mark folderRenamed`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(
            result = Result.success(SystemFolders.defaults + workFolder),
            renameResult = { _, _ -> Result.failure(RuntimeException("name taken")) },
        )
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.renameFolder(workFolder, "Projects")

        assertEquals("name taken", viewModel.state.value.folderActionError)
        assertFalse(viewModel.state.value.folderRenamed)
        assertFalse(viewModel.state.value.renamingFolder)
    }

    @Test
    fun `renameFolder ignores a second call while one is already in flight`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        val gate = CompletableDeferred<Unit>()
        folderRepository.renameGate = gate

        viewModel.renameFolder(workFolder, "Projects")
        assertTrue(viewModel.state.value.renamingFolder)

        viewModel.renameFolder(workFolder, "Personal") // should be ignored by the renamingFolder guard

        assertEquals(1, folderRepository.renameCalls.size, "second renameFolder should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.renamingFolder)
    }

    @Test
    fun `consumeFolderRenamed clears the flag`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        viewModel.renameFolder(workFolder, "Projects")
        assertTrue(viewModel.state.value.folderRenamed)

        viewModel.consumeFolderRenamed()

        assertFalse(viewModel.state.value.folderRenamed)
    }

    @Test
    fun `deleteFolder removes the folder from state and signals folderDeleted`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.deleteFolder(workFolder)

        assertEquals(listOf("mb1" to "work"), folderRepository.deleteCalls)
        assertFalse(viewModel.state.value.folders.contains(workFolder))
        assertTrue(viewModel.state.value.folderDeleted)
        assertFalse(viewModel.state.value.deletingFolder)
    }

    @Test
    fun `deleteFolder switches to Inbox and reloads when the deleted folder was selected`() = runTest {
        val emailRepository = FakeEmailRepository(
            handler = { _, folder, _, _ -> Result.success(EmailPage(listOf(summary(folder)), totalCount = 1)) },
        )
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        viewModel.selectFolder(workFolder)

        viewModel.deleteFolder(workFolder)

        assertEquals(SystemFolders.INBOX, viewModel.state.value.currentFolder.id)
        assertEquals(listOf(summary("inbox")), viewModel.state.value.emails)
    }

    @Test
    fun `deleteFolder rejects deleting a system folder without calling the repository`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository()
        val viewModel = buildViewModel(emailRepository, folderRepository)
        val inboxFolder = SystemFolders.defaults.first { it.id == SystemFolders.INBOX }

        viewModel.deleteFolder(inboxFolder)

        assertTrue(folderRepository.deleteCalls.isEmpty())
        assertEquals("System folders can't be deleted.", viewModel.state.value.folderActionError)
    }

    @Test
    fun `deleteFolder surfaces the failure and keeps the folder in state`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(
            result = Result.success(SystemFolders.defaults + workFolder),
            deleteResult = Result.failure(RuntimeException("in use")),
        )
        val viewModel = buildViewModel(emailRepository, folderRepository)

        viewModel.deleteFolder(workFolder)

        assertEquals("in use", viewModel.state.value.folderActionError)
        assertTrue(viewModel.state.value.folders.contains(workFolder))
        assertFalse(viewModel.state.value.folderDeleted)
        assertFalse(viewModel.state.value.deletingFolder)
    }

    @Test
    fun `deleteFolder ignores a second call while one is already in flight`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        val gate = CompletableDeferred<Unit>()
        folderRepository.deleteGate = gate

        viewModel.deleteFolder(workFolder)
        assertTrue(viewModel.state.value.deletingFolder)

        viewModel.deleteFolder(workFolder) // should be ignored by the deletingFolder guard

        assertEquals(1, folderRepository.deleteCalls.size, "second deleteFolder should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.deletingFolder)
    }

    @Test
    fun `consumeFolderDeleted clears the flag`() = runTest {
        val emailRepository = FakeEmailRepository()
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults + workFolder))
        val viewModel = buildViewModel(emailRepository, folderRepository)
        viewModel.deleteFolder(workFolder)
        assertTrue(viewModel.state.value.folderDeleted)

        viewModel.consumeFolderDeleted()

        assertFalse(viewModel.state.value.folderDeleted)
    }
}
