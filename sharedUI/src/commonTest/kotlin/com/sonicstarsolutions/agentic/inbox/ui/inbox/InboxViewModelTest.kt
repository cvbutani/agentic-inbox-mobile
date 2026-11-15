package com.sonicstarsolutions.agentic.inbox.ui.inbox

import com.sonicstarsolutions.agentic.inbox.domain.model.BulkAction
import com.sonicstarsolutions.agentic.inbox.domain.model.BulkOutcome
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsByLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetLabelsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.BulkUpdateEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveLiveUpdatesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveDraftsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RenameFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeDraftRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeLabelRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeLiveUpdates
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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

    private fun TestScope.buildViewModel(
        repository: FakeEmailRepository,
        folderRepository: FakeFolderRepository = FakeFolderRepository(),
        draftRepository: FakeDraftRepository = FakeDraftRepository(),
        labelRepository: FakeLabelRepository = FakeLabelRepository(),
        liveUpdates: FakeLiveUpdates = FakeLiveUpdates(),
    ): InboxViewModel = InboxViewModel(
        getEmails = GetEmailsUseCase(repository),
        getFolders = GetFoldersUseCase(folderRepository),
        createFolderUseCase = CreateFolderUseCase(folderRepository),
        renameFolderUseCase = RenameFolderUseCase(folderRepository),
        deleteFolderUseCase = DeleteFolderUseCase(folderRepository),
        setEmailStarredUseCase = SetEmailStarredUseCase(repository),
        moveEmailUseCase = MoveEmailUseCase(repository),
        deleteEmailUseCase = DeleteEmailUseCase(repository),
        bulkUpdateEmails = BulkUpdateEmailsUseCase(repository),
        getEmailsByLabel = GetEmailsByLabelUseCase(repository),
        getLabels = GetLabelsUseCase(labelRepository),
        createLabelUseCase = CreateLabelUseCase(labelRepository),
        updateLabelUseCase = UpdateLabelUseCase(labelRepository),
        deleteLabelUseCase = DeleteLabelUseCase(labelRepository),
        observeLiveUpdates = ObserveLiveUpdatesUseCase(liveUpdates),
        observeDraftsUseCase = ObserveDraftsUseCase(draftRepository),
        deleteDraftUseCase = DeleteDraftUseCase(draftRepository),
        // Stands in for the app-lifetime scope; shares the test's virtual clock so the undo
        // window can be advanced past.
        externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        mailboxId = "mb1",
        mailboxName = "Inbox",
    )

    private fun draft(id: String, mailboxId: String = "mb1") = Draft(
        id = id,
        mailboxId = mailboxId,
        to = "bob@example.dev",
        subject = "Draft $id",
        body = "text",
        mode = "NEW",
        updatedAt = 1L,
    )

    @Test
    fun `drafts for this mailbox are observed into state`() = runTest {
        val drafts = FakeDraftRepository(initial = listOf(draft("d1"), draft("d2", mailboxId = "other")))
        val viewModel = buildViewModel(FakeEmailRepository(), draftRepository = drafts)
        advanceUntilIdle()

        assertEquals(listOf("d1"), viewModel.state.value.drafts.map { it.id })
    }

    @Test
    fun `a newly saved draft appears without a refresh`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(FakeEmailRepository(), draftRepository = drafts)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.drafts.isEmpty())

        drafts.save(draft("d1"))
        advanceUntilIdle()

        assertEquals(listOf("d1"), viewModel.state.value.drafts.map { it.id })
    }

    @Test
    fun `showingDrafts is only true in the Drafts folder`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository())
        assertFalse(viewModel.state.value.showingDrafts)

        viewModel.selectFolder(SystemFolders.defaults.first { it.id == SystemFolders.DRAFT })

        assertTrue(viewModel.state.value.showingDrafts)
    }

    @Test
    fun `deleteDraft removes it from state`() = runTest {
        val drafts = FakeDraftRepository(initial = listOf(draft("d1")))
        val viewModel = buildViewModel(FakeEmailRepository(), draftRepository = drafts)
        advanceUntilIdle()

        viewModel.deleteDraft("d1")
        advanceUntilIdle()

        assertEquals(listOf("d1"), drafts.deleteCalls)
        assertTrue(viewModel.state.value.drafts.isEmpty())
    }

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

    @Test
    fun `toggleStarred optimistically flips the starred state and calls the use case`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1").copy(starred = false)), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.toggleStarred(viewModel.state.value.emails.single())

        assertTrue(viewModel.state.value.emails.single().starred)
        assertEquals(
            listOf(FakeEmailRepository.SetStarredCall("mb1", "e1", true)),
            repository.setStarredCalls,
        )
    }

    @Test
    fun `toggleStarred reverts and surfaces an error on failure`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1").copy(starred = false)), totalCount = 1)) },
            setStarredResult = Result.failure(RuntimeException("offline")),
        )
        val viewModel = buildViewModel(repository)

        viewModel.toggleStarred(viewModel.state.value.emails.single())

        assertFalse(viewModel.state.value.emails.single().starred)
        assertEquals("offline", viewModel.state.value.errorMessage)
    }

    @Test
    fun `archiveEmail removes the email from the list and moves it to Archive`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.archiveEmail(summary("e1"))

        assertEquals(listOf(summary("e2")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.totalCount)
        assertEquals(listOf(FakeEmailRepository.MoveCall("mb1", "e1", SystemFolders.ARCHIVE)), repository.moveCalls)
    }

    @Test
    fun `archiveEmail reinserts the email and surfaces an error on failure`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
            moveResult = Result.failure(RuntimeException("offline")),
        )
        val viewModel = buildViewModel(repository)

        viewModel.archiveEmail(summary("e1"))

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.emails)
        assertEquals(2, viewModel.state.value.totalCount)
        assertEquals("offline", viewModel.state.value.errorMessage)
    }

    @Test
    fun `deleteEmail removes the email from the list but holds the server call for the undo window`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e1"))

        assertEquals(listOf(summary("e2")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.totalCount)
        assertEquals(summary("e1"), viewModel.state.value.pendingDelete?.email)
        assertTrue(repository.deleteCalls.isEmpty(), "delete should not reach the server inside the undo window")

        advanceTimeBy(InboxViewModel.UNDO_WINDOW_MILLIS + 1)

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "e1")), repository.deleteCalls)
        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun `undoPendingDelete restores the email at its original position and never calls the server`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2"), summary("e3")), totalCount = 3)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e2"))
        viewModel.undoPendingDelete()
        advanceTimeBy(InboxViewModel.UNDO_WINDOW_MILLIS + 1)

        assertEquals(listOf(summary("e1"), summary("e2"), summary("e3")), viewModel.state.value.emails)
        assertEquals(3, viewModel.state.value.totalCount)
        assertNull(viewModel.state.value.pendingDelete)
        assertTrue(repository.deleteCalls.isEmpty(), "an undone delete must never reach the server")
    }

    @Test
    fun `a second delete commits the first pending delete immediately`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e1"))
        viewModel.deleteEmail(summary("e2"))

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "e1")), repository.deleteCalls)
        assertEquals(summary("e2"), viewModel.state.value.pendingDelete?.email)
    }

    @Test
    fun `onRefresh commits a pending delete rather than letting the row come back`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e1"))
        viewModel.onRefresh()

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "e1")), repository.deleteCalls)
        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun `a staged delete still reaches the server if the screen goes away first`() = runTest {
        // The user has already been told the email was deleted, so leaving the inbox inside the
        // undo window must not quietly cancel it — on viewModelScope this delete would be lost
        // and the row would reappear on the next load.
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e1"))
        viewModel.viewModelScope.cancel()
        advanceTimeBy(InboxViewModel.UNDO_WINDOW_MILLIS + 1)

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "e1")), repository.deleteCalls)
    }

    @Test
    fun `a committed delete that the server rejects reinserts the email and surfaces an error`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
            deleteResult = Result.failure(RuntimeException("offline")),
        )
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(summary("e1"))
        advanceTimeBy(InboxViewModel.UNDO_WINDOW_MILLIS + 1)

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.emails)
        assertEquals(2, viewModel.state.value.totalCount)
        assertEquals("offline", viewModel.state.value.errorMessage)
    }

    @Test
    fun `enterSelectionMode turns on selection mode with the given email selected`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository())

        viewModel.enterSelectionMode("e1")

        assertTrue(viewModel.state.value.selectionMode)
        assertEquals(setOf("e1"), viewModel.state.value.selectedEmailIds)
    }

    @Test
    fun `toggleSelection adds and removes ids exiting selection mode when empty`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository())
        viewModel.enterSelectionMode("e1")

        viewModel.toggleSelection("e2")
        assertEquals(setOf("e1", "e2"), viewModel.state.value.selectedEmailIds)

        viewModel.toggleSelection("e1")
        assertEquals(setOf("e2"), viewModel.state.value.selectedEmailIds)
        assertTrue(viewModel.state.value.selectionMode)

        viewModel.toggleSelection("e2")
        assertTrue(viewModel.state.value.selectedEmailIds.isEmpty())
        assertFalse(viewModel.state.value.selectionMode)
    }

    @Test
    fun `selectAll selects every loaded email`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.selectAll()

        assertEquals(setOf("e1", "e2"), viewModel.state.value.selectedEmailIds)
    }

    @Test
    fun `clearSelection exits selection mode and clears the selection`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository())
        viewModel.enterSelectionMode("e1")

        viewModel.clearSelection()

        assertFalse(viewModel.state.value.selectionMode)
        assertTrue(viewModel.state.value.selectedEmailIds.isEmpty())
    }

    private fun TestScope.twoSelected(repository: FakeEmailRepository): InboxViewModel {
        val viewModel = buildViewModel(repository)
        viewModel.enterSelectionMode("e1")
        viewModel.toggleSelection("e2")
        return viewModel
    }

    private fun pageOf(vararg emails: EmailSummary) =
        { _: String, _: String, _: Int, _: Int -> Result.success(EmailPage(emails.toList(), totalCount = emails.size)) }

    @Test
    fun `batchArchive moves the selection to Archive in one request removes them and exits selection mode`() = runTest {
        val repository = FakeEmailRepository(handler = pageOf(summary("e1"), summary("e2"), summary("e3")))
        val viewModel = twoSelected(repository)

        viewModel.batchArchive()

        assertEquals(
            listOf(FakeEmailRepository.BulkCall("mb1", BulkAction.MOVE, setOf("e1", "e2"), SystemFolders.ARCHIVE, folder = "inbox")),
            repository.bulkCalls,
        )
        assertTrue(repository.moveCalls.isEmpty(), "per-email moves must not be used")
        assertEquals(listOf(summary("e3")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.totalCount)
        assertFalse(viewModel.state.value.selectionMode)
        assertTrue(viewModel.state.value.selectedEmailIds.isEmpty())
    }

    @Test
    fun `batchArchive surfaces an error and keeps the emails when the request fails`() = runTest {
        val repository = FakeEmailRepository(
            handler = pageOf(summary("e1"), summary("e2")),
            bulkHandler = { _, ids -> BulkOutcome(succeeded = emptySet(), failed = ids.toSet()) },
        )
        val viewModel = twoSelected(repository)

        viewModel.batchArchive()

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.emails)
        assertEquals(2, viewModel.state.value.totalCount)
        assertEquals("Failed to update 2 email(s)", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.selectionMode)
    }

    @Test
    fun `a partly failed batch removes only the emails that succeeded`() = runTest {
        val repository = FakeEmailRepository(
            handler = pageOf(summary("e1"), summary("e2")),
            bulkHandler = { _, _ -> BulkOutcome(succeeded = setOf("e1"), failed = setOf("e2")) },
        )
        val viewModel = twoSelected(repository)

        viewModel.batchDelete()

        assertEquals(listOf(summary("e2")), viewModel.state.value.emails)
        assertEquals(1, viewModel.state.value.totalCount)
        assertEquals("Failed to update 1 email(s)", viewModel.state.value.errorMessage)
    }

    @Test
    fun `batchDelete deletes the selection in one request removes them and exits selection mode`() = runTest {
        val repository = FakeEmailRepository(handler = pageOf(summary("e1"), summary("e2")))
        val viewModel = twoSelected(repository)

        viewModel.batchDelete()

        assertEquals(
            listOf(FakeEmailRepository.BulkCall("mb1", BulkAction.DELETE, setOf("e1", "e2"), null, folder = "inbox")),
            repository.bulkCalls,
        )
        assertTrue(repository.deleteCalls.isEmpty(), "per-email deletes must not be used")
        assertTrue(viewModel.state.value.emails.isEmpty())
        assertEquals(0, viewModel.state.value.totalCount)
        assertFalse(viewModel.state.value.selectionMode)
    }

    @Test
    fun `batchMarkAsRead marks the selection read in one request without removing them`() = runTest {
        val repository = FakeEmailRepository(handler = pageOf(summary("e1").copy(read = false), summary("e2").copy(read = false)))
        val viewModel = twoSelected(repository)

        viewModel.batchMarkAsRead()

        assertEquals(
            listOf(FakeEmailRepository.BulkCall("mb1", BulkAction.READ, setOf("e1", "e2"), null, folder = "inbox")),
            repository.bulkCalls,
        )
        assertTrue(viewModel.state.value.emails.all { it.read })
        assertEquals(2, viewModel.state.value.emails.size)
        assertFalse(viewModel.state.value.selectionMode)
    }

    @Test
    fun `batchMarkAsUnread marks the selection unread in one request`() = runTest {
        val repository = FakeEmailRepository(handler = pageOf(summary("e1").copy(read = true), summary("e2").copy(read = true)))
        val viewModel = twoSelected(repository)

        viewModel.batchMarkAsUnread()

        assertEquals(
            listOf(FakeEmailRepository.BulkCall("mb1", BulkAction.UNREAD, setOf("e1", "e2"), null, folder = "inbox")),
            repository.bulkCalls,
        )
        assertTrue(viewModel.state.value.emails.none { it.read })
        assertFalse(viewModel.state.value.selectionMode)
    }

    // --- labels ----------------------------------------------------------------------------

    private val work = Label("work", "Work", "blue")
    private val fun_ = Label("fun", "Fun", "red")

    @Test
    fun `the mailbox's labels are loaded into state`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = FakeLabelRepository(Result.success(listOf(work, fun_))))
        advanceUntilIdle()

        assertEquals(listOf(work, fun_), viewModel.state.value.labels)
    }

    @Test
    fun `a failed label lookup leaves the drawer without labels and shows no error`() = runTest {
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = FakeLabelRepository(Result.failure(RuntimeException("offline"))))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.labels.isEmpty())
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `selectLabel shows that label's emails instead of a folder`() = runTest {
        val repository = FakeEmailRepository(
            labelPageHandler = { _, _, _ -> Result.success(EmailPage(listOf(summary("l1"), summary("l2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository, labelRepository = FakeLabelRepository(Result.success(listOf(work))))

        viewModel.selectLabel(work)

        assertEquals(work, viewModel.state.value.currentLabel)
        assertEquals(listOf("l1", "l2"), viewModel.state.value.emails.map { it.id })
        assertEquals(listOf(FakeEmailRepository.LabelPageCall("mb1", "work", 1, 50)), repository.labelPageCalls)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `selecting a folder leaves the label view`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        viewModel.selectLabel(work)

        viewModel.selectFolder(SystemFolders.defaults.first { it.id == SystemFolders.ARCHIVE })

        assertNull(viewModel.state.value.currentLabel)
        assertEquals("archive", viewModel.state.value.currentFolder.id)
        assertEquals("archive", repository.calls.last().folder)
    }

    @Test
    fun `refresh and load more keep using the label query while a label is open`() = runTest {
        val repository = FakeEmailRepository(
            labelPageHandler = { _, page, _ -> Result.success(EmailPage(listOf(summary("p$page")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.selectLabel(work)

        viewModel.loadMore()
        viewModel.onRefresh()
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 1), repository.labelPageCalls.map { it.page })
        assertTrue(repository.calls.none { it.page == 2 }, "a folder page must not be fetched for a label view")
    }

    @Test
    fun `createLabel adds the label and signals the dialog to close`() = runTest {
        val labels = FakeLabelRepository()
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)

        viewModel.createLabel("  Client work ", "teal")

        assertEquals(listOf("Client work" to "teal"), labels.createCalls)
        assertEquals(listOf("client work"), viewModel.state.value.labels.map { it.id })
        assertTrue(viewModel.state.value.labelSaved)
    }

    @Test
    fun `createLabel rejects a blank name without calling the server`() = runTest {
        val labels = FakeLabelRepository()
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)

        viewModel.createLabel("   ", "teal")

        assertTrue(labels.createCalls.isEmpty())
        assertEquals("Label name is required.", viewModel.state.value.labelActionError)
    }

    @Test
    fun `createLabel shows the server's message when it fails`() = runTest {
        val labels = FakeLabelRepository(createResult = { _, _ -> Result.failure(RuntimeException("A label with this name already exists")) })
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)

        viewModel.createLabel("Work", "blue")

        assertEquals("A label with this name already exists", viewModel.state.value.labelActionError)
        assertFalse(viewModel.state.value.labelSaved)
        assertTrue(viewModel.state.value.labels.isEmpty())
    }

    @Test
    fun `updateLabel changes the label in the list and the open label view`() = runTest {
        val labels = FakeLabelRepository(Result.success(listOf(work, fun_)))
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)
        viewModel.selectLabel(work)

        viewModel.updateLabel(work, name = "Office", color = "purple")

        val renamed = Label("work", "Office", "purple")
        assertEquals(listOf("fun" to "Fun", "work" to "Office"), viewModel.state.value.labels.map { it.id to it.name })
        assertEquals(renamed, viewModel.state.value.currentLabel)
        assertTrue(viewModel.state.value.labelSaved)
    }

    @Test
    fun `updateLabel only sends what changed`() = runTest {
        val labels = FakeLabelRepository(Result.success(listOf(work)))
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)

        viewModel.updateLabel(work, name = "Work", color = "red")

        assertEquals(listOf(Triple<String, String?, String?>("work", null, "red")), labels.updateCalls)
    }

    @Test
    fun `deleteLabel removes it and returns to the inbox when it was open`() = runTest {
        val labels = FakeLabelRepository(Result.success(listOf(work, fun_)))
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)
        viewModel.selectLabel(work)

        viewModel.deleteLabel(work)

        assertEquals(listOf("work"), labels.deleteCalls)
        assertEquals(listOf(fun_), viewModel.state.value.labels)
        assertNull(viewModel.state.value.currentLabel)
        assertEquals("inbox", viewModel.state.value.currentFolder.id)
    }

    @Test
    fun `deleteLabel keeps the label and reports the error when it fails`() = runTest {
        val labels = FakeLabelRepository(Result.success(listOf(work)), deleteResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(FakeEmailRepository(), labelRepository = labels)

        viewModel.deleteLabel(work)

        assertEquals(listOf(work), viewModel.state.value.labels)
        assertEquals("offline", viewModel.state.value.labelActionError)
    }

    // --- applying labels to a selection ----------------------------------------------------

    private fun labelled(id: String, vararg labels: Label) = summary(id).copy(labels = labels.toList())

    @Test
    fun `a label's check state reflects how many selected emails carry it`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(labelled("e1", work, fun_), labelled("e2", work), labelled("e3")), totalCount = 3)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.enterSelectionMode("e1")
        viewModel.toggleSelection("e2")

        assertEquals(LabelCheck.ALL, viewModel.state.value.labelCheck("work"))
        assertEquals(LabelCheck.SOME, viewModel.state.value.labelCheck("fun"))
        assertEquals(LabelCheck.NONE, viewModel.state.value.labelCheck("other"))
    }

    @Test
    fun `applyLabelChanges labels and unlabels the selection in bulk requests`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(labelled("e1", fun_), labelled("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository, labelRepository = FakeLabelRepository(Result.success(listOf(work, fun_))))
        viewModel.enterSelectionMode("e1")
        viewModel.toggleSelection("e2")

        viewModel.applyLabelChanges(mapOf("work" to true, "fun" to false))

        assertEquals(
            setOf(
                FakeEmailRepository.BulkCall("mb1", BulkAction.LABEL, setOf("e1", "e2"), null, "work", folder = "inbox"),
                FakeEmailRepository.BulkCall("mb1", BulkAction.UNLABEL, setOf("e1", "e2"), null, "fun", folder = "inbox"),
            ),
            repository.bulkCalls.toSet(),
        )
        val emails = viewModel.state.value.emails
        assertEquals(listOf(work), emails.first { it.id == "e1" }.labels)
        assertEquals(listOf(work), emails.first { it.id == "e2" }.labels)
        assertFalse(viewModel.state.value.selectionMode)
    }

    @Test
    fun `a failed label request leaves the labels alone and reports it`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(labelled("e1")), totalCount = 1)) },
            bulkHandler = { _, ids -> BulkOutcome(emptySet(), ids.toSet()) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.enterSelectionMode("e1")

        viewModel.applyLabelChanges(mapOf("work" to true))

        assertTrue(viewModel.state.value.emails.single().labels.isEmpty())
        assertEquals("Failed to update 1 email(s)", viewModel.state.value.errorMessage)
    }

    @Test
    fun `removing the open label from emails drops them from that label's list`() = runTest {
        val repository = FakeEmailRepository(
            labelPageHandler = { _, _, _ -> Result.success(EmailPage(listOf(labelled("e1", work), labelled("e2", work)), 2)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.selectLabel(work)
        viewModel.enterSelectionMode("e1")

        viewModel.applyLabelChanges(mapOf("work" to false))

        assertEquals(listOf("e2"), viewModel.state.value.emails.map { it.id })
        assertEquals(1, viewModel.state.value.totalCount)
    }

    // --- live updates ----------------------------------------------------------------------

    private val tick get() = InboxViewModel.LIVE_DEBOUNCE_MILLIS + 1

    private fun TestScope.listening(
        repository: FakeEmailRepository,
        live: FakeLiveUpdates = FakeLiveUpdates(),
        folders: FakeFolderRepository = FakeFolderRepository(),
        labels: FakeLabelRepository = FakeLabelRepository(),
    ): InboxViewModel {
        val viewModel = buildViewModel(repository, folderRepository = folders, labelRepository = labels, liveUpdates = live)
        backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)
        return viewModel
    }

    @Test
    fun `a change notice reloads the first page without showing a spinner`() = runTest {
        var served = listOf(summary("old"))
        val repository = FakeEmailRepository(handler = { _, _, _, _ -> Result.success(EmailPage(served, served.size)) })
        val live = FakeLiveUpdates()
        val viewModel = listening(repository, live)
        assertEquals(listOf("old"), viewModel.state.value.emails.map { it.id })

        served = listOf(summary("new"), summary("old"))
        live.emit(LiveScope.EMAILS)
        advanceTimeBy(tick)

        assertEquals(listOf("new", "old"), viewModel.state.value.emails.map { it.id })
        assertEquals(2, viewModel.state.value.totalCount)
        assertFalse(viewModel.state.value.refreshing)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `a burst of notices causes one reload`() = runTest {
        val repository = FakeEmailRepository()
        val live = FakeLiveUpdates()
        listening(repository, live)
        val before = repository.calls.size

        repeat(5) { live.emit(LiveScope.EMAILS); advanceTimeBy(InboxViewModel.LIVE_DEBOUNCE_MILLIS / 5) }
        advanceTimeBy(tick)

        assertEquals(before + 1, repository.calls.size)
    }

    @Test
    fun `a change to the scheduled queue does not reload the inbox`() = runTest {
        val repository = FakeEmailRepository()
        val live = FakeLiveUpdates()
        listening(repository, live)
        val before = repository.calls.size

        live.emit(LiveScope.SCHEDULED)
        advanceTimeBy(tick)

        assertEquals(before, repository.calls.size)
    }

    @Test
    fun `a notice also refreshes folder counts and label counts`() = runTest {
        val folders = FakeFolderRepository()
        val labels = FakeLabelRepository()
        val live = FakeLiveUpdates()
        listening(FakeEmailRepository(), live, folders, labels)
        val (folderCalls, labelCalls) = folders.getFoldersCalls to labels.getLabelsCalls

        live.emit(LiveScope.FOLDERS)
        advanceTimeBy(tick)

        assertEquals(folderCalls + 1, folders.getFoldersCalls)
        assertEquals(labelCalls + 1, labels.getLabelsCalls)
    }

    @Test
    fun `a notice while a swiped delete can still be undone leaves the list alone`() = runTest {
        val repository = FakeEmailRepository(handler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), 2)) })
        val live = FakeLiveUpdates()
        val viewModel = listening(repository, live)
        viewModel.deleteEmail(summary("e1"))
        val before = repository.calls.size

        live.emit(LiveScope.EMAILS)
        advanceTimeBy(tick)

        assertEquals(before, repository.calls.size)
        assertEquals(listOf("e2"), viewModel.state.value.emails.map { it.id })
    }

    @Test
    fun `when more than the first page is loaded the list is left to the user`() = runTest {
        val repository = FakeEmailRepository(handler = { _, _, page, _ -> Result.success(EmailPage(listOf(summary("p$page")), totalCount = 2)) })
        val live = FakeLiveUpdates()
        val viewModel = listening(repository, live)
        viewModel.loadMore()
        advanceUntilIdle()
        val before = repository.calls.size

        live.emit(LiveScope.EMAILS)
        advanceTimeBy(tick)

        // Reloading page one would throw away the pages the user scrolled through.
        assertEquals(before, repository.calls.size)
        assertEquals(listOf("p1", "p2"), viewModel.state.value.emails.map { it.id })
    }

    @Test
    fun `an open label view is reloaded through the label query`() = runTest {
        val repository = FakeEmailRepository()
        val live = FakeLiveUpdates()
        val viewModel = listening(repository, live)
        viewModel.selectLabel(work)
        val before = repository.labelPageCalls.size

        live.emit(LiveScope.EMAILS)
        advanceTimeBy(tick)

        assertEquals(before + 1, repository.labelPageCalls.size)
    }

    @Test
    fun `a failed quiet reload keeps the list and shows no error`() = runTest {
        var fail = false
        val repository = FakeEmailRepository(handler = { _, _, _, _ ->
            if (fail) Result.failure(RuntimeException("offline")) else Result.success(EmailPage(listOf(summary("a")), 1))
        })
        val live = FakeLiveUpdates()
        val viewModel = listening(repository, live)

        fail = true
        live.emit(LiveScope.EMAILS)
        advanceTimeBy(tick)

        assertEquals(listOf("a"), viewModel.state.value.emails.map { it.id })
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `listening starts a connection and stopping it ends the connection`() = runTest {
        val live = FakeLiveUpdates()
        val viewModel = buildViewModel(FakeEmailRepository(), liveUpdates = live)
        val job = backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)
        assertEquals(1, live.collectors)

        job.cancel()
        advanceTimeBy(tick)

        // A cancelled collector must not leave a reload running behind it.
        assertTrue(job.isCancelled)
    }

    @Test
    fun `coming back after the connection was closed catches up on what was missed`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        advanceUntilIdle() // initial load done
        val before = repository.calls.size

        val job = backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)

        assertEquals(before + 1, repository.calls.size)
        job.cancel()
    }

    // --- conversations ---------------------------------------------------------------------

    private fun conversation(id: String, thread: String, count: Int) = summary(id).copy(threadId = thread, threadCount = count)

    private fun TestScope.conversationInbox(repository: FakeEmailRepository = inboxOf(conversation("c1", "t1", 3), summary("s1"))) = buildViewModel(repository)

    private fun inboxOf(vararg emails: EmailSummary) =
        FakeEmailRepository(handler = { _, _, _, _ -> Result.success(EmailPage(emails.toList(), emails.size)) })

    @Test
    fun `batch archive sends a conversation row by thread and a single message by id`() = runTest {
        val repository = inboxOf(conversation("c1", "t1", 3), summary("s1"))
        val viewModel = buildViewModel(repository)
        viewModel.enterSelectionMode("c1")
        viewModel.toggleSelection("s1")

        viewModel.batchArchive()

        val call = repository.bulkCalls.single()
        assertEquals(setOf("c1", "s1"), call.ids)
        assertEquals(mapOf("c1" to "t1"), call.conversations)
        assertEquals("inbox", call.folder)
        assertEquals(SystemFolders.ARCHIVE, call.folderId)
    }

    @Test
    fun `batch mark read covers the whole conversation`() = runTest {
        val repository = inboxOf(conversation("c1", "t1", 2))
        val viewModel = buildViewModel(repository)
        viewModel.enterSelectionMode("c1")

        viewModel.batchMarkAsRead()

        assertEquals(mapOf("c1" to "t1"), repository.bulkCalls.single().conversations)
    }

    @Test
    fun `swiping a conversation to archive moves the whole conversation`() = runTest {
        val repository = inboxOf(conversation("c1", "t1", 3))
        val viewModel = buildViewModel(repository)

        viewModel.archiveEmail(conversation("c1", "t1", 3))

        assertTrue(repository.moveCalls.isEmpty(), "a lone message move would leave the rest of the conversation behind")
        val call = repository.bulkCalls.single()
        assertEquals(BulkAction.MOVE, call.action)
        assertEquals(mapOf("c1" to "t1"), call.conversations)
        assertEquals(SystemFolders.ARCHIVE, call.folderId)
        assertTrue(viewModel.state.value.emails.none { it.id == "c1" })
    }

    @Test
    fun `swiping a single message to archive still moves just that message`() = runTest {
        val repository = inboxOf(summary("s1"))
        val viewModel = buildViewModel(repository)

        viewModel.archiveEmail(summary("s1"))

        assertEquals(listOf(FakeEmailRepository.MoveCall("mb1", "s1", SystemFolders.ARCHIVE)), repository.moveCalls)
        assertTrue(repository.bulkCalls.isEmpty())
    }

    @Test
    fun `a failed conversation archive puts the row back`() = runTest {
        val repository = FakeEmailRepository(
            handler = { _, _, _, _ -> Result.success(EmailPage(listOf(conversation("c1", "t1", 3)), 1)) },
            bulkHandler = { _, ids -> BulkOutcome(emptySet(), ids.toSet()) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.archiveEmail(conversation("c1", "t1", 3))

        assertEquals(listOf("c1"), viewModel.state.value.emails.map { it.id })
        assertTrue(viewModel.state.value.errorMessage != null)
    }

    @Test
    fun `a swipe-deleted conversation is deleted as a whole once the undo window passes`() = runTest {
        val repository = inboxOf(conversation("c1", "t1", 3))
        val viewModel = buildViewModel(repository)

        viewModel.deleteEmail(conversation("c1", "t1", 3))
        advanceTimeBy(InboxViewModel.UNDO_WINDOW_MILLIS + 1)

        assertTrue(repository.deleteCalls.isEmpty())
        val call = repository.bulkCalls.single()
        assertEquals(BulkAction.DELETE, call.action)
        assertEquals(mapOf("c1" to "t1"), call.conversations)
    }

    @Test
    fun `rows in a label view are single messages, so nothing is sent by thread`() = runTest {
        val repository = FakeEmailRepository(
            labelPageHandler = { _, _, _ -> Result.success(EmailPage(listOf(conversation("c1", "t1", 3)), 1)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.selectLabel(work)
        viewModel.enterSelectionMode("c1")

        viewModel.batchMarkAsRead()

        assertTrue(repository.bulkCalls.single().conversations.isEmpty())
        assertNull(repository.bulkCalls.single().folder)
    }

    @Test
    fun `toggleRead marks an unread message read and keeps it in the list`() = runTest {
        val unread = summary("s1").copy(read = false)
        val repository = inboxOf(unread)
        val viewModel = buildViewModel(repository)

        viewModel.toggleRead(unread)

        assertEquals(BulkAction.READ, repository.bulkCalls.single().action)
        assertEquals(setOf("s1"), repository.bulkCalls.single().ids)
        assertTrue(viewModel.state.value.emails.single().read)
    }

    @Test
    fun `toggleRead marks a read message unread`() = runTest {
        val read = summary("s1").copy(read = true)
        val repository = inboxOf(read)
        val viewModel = buildViewModel(repository)

        viewModel.toggleRead(read)

        assertEquals(BulkAction.UNREAD, repository.bulkCalls.single().action)
        assertFalse(viewModel.state.value.emails.single().read)
    }

    @Test
    fun `toggleRead on a conversation with unread messages marks the whole conversation read`() = runTest {
        val repository = inboxOf(conversation("c1", "t1", 3))
        val viewModel = buildViewModel(repository)

        viewModel.toggleRead(conversation("c1", "t1", 3))

        assertEquals(BulkAction.READ, repository.bulkCalls.single().action)
        assertEquals(mapOf("c1" to "t1"), repository.bulkCalls.single().conversations)
    }
}
