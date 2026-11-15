package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.domain.model.BulkAction
import com.sonicstarsolutions.agentic.inbox.domain.model.BulkOutcome
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.repository.AttachmentRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ScheduleRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ContactRepository
import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledStatus
import kotlin.time.Instant
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * SCREENSHOT-ONLY MODULE — not meant to be committed.
 *
 * Wires every repository to static, fictional data (example.com addresses, no network or
 * database access) so store screenshots never show real mailbox content. To use: swap
 * [demoRepositoryModule] in for [repositoryModule] in AppModule.commonModules, build, take
 * screenshots, then `git checkout` both this file's addition and that edit away.
 */

private val now = Clock.System.now()
private const val DEMO_MAILBOX_ID = "demo"
private const val DEMO_EMAIL = "you@example.com"

private data class DemoMessage(
    val id: String,
    val folderId: String,
    val threadId: String?,
    val subject: String,
    val sender: String,
    val recipient: String = "You <$DEMO_EMAIL>",
    val ageAgo: Duration,
    val read: Boolean,
    val starred: Boolean,
    val snippet: String,
    val body: String,
)

private val messagesById: MutableMap<String, DemoMessage> = listOf(
    DemoMessage(
        id = "m1", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Your weekly summary is ready",
        sender = "Agentic Inbox <digest@example.com>",
        ageAgo = 25.minutes, read = false, starred = false,
        snippet = "12 new messages, 3 flagged for follow-up this week.",
        body = "Here's what happened in your inbox this week:\n\n" +
            "• 12 new messages across 4 mailboxes\n• 3 flagged for follow-up\n" +
            "• 2 threads waiting on your reply\n\nHave a great week!",
    ),
    DemoMessage(
        id = "m2", folderId = SystemFolders.INBOX, threadId = "t-design",
        subject = "Design review: onboarding flow",
        sender = "Ava Chen <ava@example.com>",
        ageAgo = 2.hours, read = false, starred = true,
        snippet = "Left a few comments on the new onboarding screens, mostly around …",
        body = "Hi team,\n\nLeft a few comments on the new onboarding screens, mostly around " +
            "spacing and the empty state copy. Nothing blocking — should be quick to address." +
            "\n\nCan we sync tomorrow?\n\nAva",
    ),
    DemoMessage(
        id = "m3", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Receipt for your recent purchase",
        sender = "Orders <receipts@example.com>",
        ageAgo = 5.hours, read = true, starred = false,
        snippet = "Thanks for your order! Your receipt is attached.",
        body = "Thanks for your order!\n\nOrder #48213\nTotal: \$42.00\n\n" +
            "Your receipt is attached for your records.",
    ),
    DemoMessage(
        id = "m4", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Weekend trip — quick question",
        sender = "Marcus Reed <marcus@example.com>",
        ageAgo = 9.hours, read = true, starred = false,
        snippet = "Hey, are we still on for Saturday? Let me know if the time …",
        body = "Hey,\n\nAre we still on for Saturday? Let me know if the time still works for " +
            "you — happy to shift earlier if needed.\n\nMarcus",
    ),
    DemoMessage(
        id = "m5", folderId = SystemFolders.INBOX, threadId = null,
        subject = "New comment on your post",
        sender = "Community <no-reply@example.com>",
        ageAgo = 1.days, read = true, starred = false,
        snippet = "Priya left a comment: \"This is exactly what I was looking for…\"",
        body = "Priya left a comment on your post:\n\n\"This is exactly what I was looking for, " +
            "thank you for sharing!\"",
    ),
    DemoMessage(
        id = "m6", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Invoice #2291 is due in 5 days",
        sender = "Billing <billing@example.com>",
        ageAgo = 1.days + 4.hours, read = false, starred = true,
        snippet = "Your invoice for \$128.00 is due on the 15th.",
        body = "Your invoice for \$128.00 is due on the 15th.\n\nView and pay online at your convenience.",
    ),
    DemoMessage(
        id = "m7", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Welcome to the team!",
        sender = "People Ops <people@example.com>",
        ageAgo = 2.days, read = true, starred = false,
        snippet = "We're excited to have you join us. Here's everything you need …",
        body = "We're excited to have you join us!\n\nHere's everything you need for your first " +
            "week, including your onboarding checklist and team directory.",
    ),
    DemoMessage(
        id = "m8", folderId = SystemFolders.INBOX, threadId = null,
        subject = "Flight confirmation — SFO to JFK",
        sender = "Travel <bookings@example.com>",
        ageAgo = 3.days, read = true, starred = false,
        snippet = "Your itinerary is confirmed for next Tuesday.",
        body = "Your itinerary is confirmed:\n\nSFO → JFK, Tuesday, 8:15 AM\nConfirmation code: X7K2QP",
    ),
    DemoMessage(
        id = "m9", folderId = SystemFolders.INBOX, threadId = "t-design",
        subject = "Re: Design review: onboarding flow",
        sender = "You <$DEMO_EMAIL>",
        recipient = "Ava Chen <ava@example.com>",
        ageAgo = 90.minutes, read = true, starred = false,
        snippet = "Thanks Ava — addressed the spacing notes, PR is up.",
        body = "Thanks Ava — addressed the spacing notes, PR is up for review.\n\n" +
            "Will take a look at the empty state copy next.",
    ),
    DemoMessage(
        id = "m10", folderId = SystemFolders.INBOX, threadId = "t-design",
        subject = "Re: Design review: onboarding flow",
        sender = "Ava Chen <ava@example.com>",
        ageAgo = 40.minutes, read = false, starred = false,
        snippet = "Looks great! One last nit on the button copy …",
        body = "Looks great! One last nit on the button copy on the final screen — otherwise ready to ship.",
    ),
    DemoMessage(
        id = "s1", folderId = SystemFolders.SENT, threadId = null,
        subject = "Re: Quarterly planning doc",
        sender = "You <$DEMO_EMAIL>",
        recipient = "Priya Nair <priya@example.com>",
        ageAgo = 6.hours, read = true, starred = false,
        snippet = "Added my notes in section 3, looks good otherwise.",
        body = "Added my notes in section 3, looks good otherwise. Let me know if you want to review together.",
    ),
    DemoMessage(
        id = "s2", folderId = SystemFolders.SENT, threadId = null,
        subject = "Introduction",
        sender = "You <$DEMO_EMAIL>",
        recipient = "Jordan Lee <jordan@example.com>",
        ageAgo = 2.days, read = true, starred = false,
        snippet = "Great meeting you at the conference — following up as promised.",
        body = "Great meeting you at the conference — following up as promised. Would love " +
            "to find time to chat further.",
    ),
    DemoMessage(
        id = "a1", folderId = SystemFolders.ARCHIVE, threadId = null,
        subject = "Last month's summary",
        sender = "Agentic Inbox <digest@example.com>",
        ageAgo = 20.days, read = true, starred = false,
        snippet = "Your monthly activity summary is ready to view.",
        body = "Your monthly activity summary is ready to view.",
    ),
    DemoMessage(
        id = "j1", folderId = SystemFolders.SPAM, threadId = null,
        subject = "You've won a prize!",
        sender = "Promotions <deals@example.com>",
        ageAgo = 4.days, read = true, starred = false,
        snippet = "Claim your reward before it expires — act now!",
        body = "Claim your reward before it expires — act now!",
    ),
).associateBy { it.id }.toMutableMap()

private fun DemoMessage.toSummary() = EmailSummary(
    id = id,
    subject = subject,
    sender = sender,
    recipient = recipient,
    date = (now - ageAgo).toString(),
    read = read,
    starred = starred,
    threadId = threadId,
    folderId = folderId,
    snippet = snippet,
)

private fun DemoMessage.toDetail() = EmailDetail(
    id = id,
    subject = subject,
    sender = sender,
    recipient = recipient,
    cc = null,
    bcc = null,
    date = (now - ageAgo).toString(),
    read = read,
    starred = starred,
    threadId = threadId,
    folderId = folderId,
    body = body,
    attachments = if (id == "m3") {
        listOf(EmailAttachment(id = "att1", filename = "receipt.pdf", mimetype = "application/pdf", size = 84_213L))
    } else {
        emptyList()
    },
)

private val demoDrafts = MutableStateFlow(
    listOf(
        Draft(
            id = "d1", mailboxId = DEMO_MAILBOX_ID,
            to = "sam@example.com", subject = "Follow-up from today",
            body = "Hi Sam,\n\nJust following up on our conversation earlier —",
            mode = "NEW", updatedAt = (now - 3.hours).toEpochMilliseconds(),
        ),
        Draft(
            id = "d2", mailboxId = DEMO_MAILBOX_ID,
            to = "team@example.com", subject = "Re: Sprint retro notes",
            body = "Thanks everyone for the notes — a few thoughts:\n\n1. ",
            mode = "REPLY", originalEmailId = "m2", threadId = "t-design",
            updatedAt = (now - 1.days).toEpochMilliseconds(),
        ),
    ),
)

class DemoCredentialsRepository : CredentialsRepository {
    private val _state = MutableStateFlow(
        Credentials(baseUrl = "https://demo.example.com", clientId = "demo-client", clientSecret = "demo-secret"),
    )
    override val state: StateFlow<Credentials> = _state.asStateFlow()
    override suspend fun stage(credentials: Credentials) { _state.value = credentials }
    override suspend fun save(credentials: Credentials) { _state.value = credentials }
    override suspend fun clear() { _state.value = Credentials() }
    override suspend fun loadIntoState() { /* already hydrated above */ }
}

class DemoConnectionRepository : ConnectionRepository {
    override suspend fun validate(): Result<Unit> = Result.success(Unit)
}

class DemoMailboxRepository : MailboxRepository {
    private val mailbox = Mailbox(id = DEMO_MAILBOX_ID, email = DEMO_EMAIL, name = "Demo Mailbox", fromName = "You")
    override suspend fun getMailboxes(): Result<List<Mailbox>> = Result.success(listOf(mailbox))
    override suspend fun createMailbox(email: String, name: String): Result<Mailbox> =
        Result.success(mailbox.copy(email = email, name = name))
    override suspend fun getAllowedDomains(): Result<List<String>> = Result.success(listOf("example.com"))
    override suspend fun getMailbox(mailboxId: String): Result<Mailbox> = Result.success(mailbox)
    override suspend fun deleteMailbox(mailboxId: String): Result<Unit> = Result.success(Unit)
    private var settings = MailboxSettings(fromName = "You")
    override suspend fun getMailboxSettings(mailboxId: String): Result<MailboxSettings> = Result.success(settings)
    override suspend fun updateMailboxSettings(mailboxId: String, settings: MailboxSettings): Result<MailboxSettings> {
        this.settings = settings
        return Result.success(settings)
    }
    override suspend fun clearCache() { /* nothing cached */ }
}

class DemoFolderRepository : FolderRepository {
    override suspend fun getFolders(mailboxId: String): Result<List<Folder>> {
        val unreadByFolder = messagesById.values.filter { !it.read }.groupingBy { it.folderId }.eachCount()
        val folders = SystemFolders.defaults.map { folder ->
            val unread = if (folder.id == SystemFolders.DRAFT) demoDrafts.value.size else unreadByFolder[folder.id] ?: 0
            folder.copy(unreadCount = unread)
        }
        return Result.success(folders)
    }
    override suspend fun createFolder(mailboxId: String, name: String): Result<Folder> =
        Result.success(Folder(id = name.lowercase(), name = name, isSystem = false))
    override suspend fun renameFolder(mailboxId: String, folderId: String, name: String): Result<Folder> =
        Result.success(Folder(id = folderId, name = name, isSystem = false))
    override suspend fun deleteFolder(mailboxId: String, folderId: String): Result<Unit> = Result.success(Unit)
    override suspend fun clearCache() { /* nothing cached */ }
}

class DemoEmailRepository : EmailRepository {
    override suspend fun getEmails(mailboxId: String, folder: String, page: Int, limit: Int): Result<EmailPage> {
        if (page > 1) return Result.success(EmailPage(emptyList(), messagesById.size))
        val emails = messagesById.values.filter { it.folderId == folder }.sortedBy { it.ageAgo }.map { it.toSummary() }
        return Result.success(EmailPage(emails, emails.size))
    }

    override suspend fun search(mailboxId: String, query: SearchQuery, page: Int, limit: Int): Result<EmailPage> {
        if (page > 1) return Result.success(EmailPage(emptyList(), 0))
        val needle = query.query.trim().lowercase()
        val emails = messagesById.values
            .filter { needle.isEmpty() || it.subject.lowercase().contains(needle) || it.snippet.lowercase().contains(needle) }
            .sortedBy { it.ageAgo }
            .map { it.toSummary() }
        return Result.success(EmailPage(emails, emails.size))
    }


    override suspend fun getEmailsByLabel(mailboxId: String, labelId: String, page: Int, limit: Int): Result<EmailPage> =
        Result.success(EmailPage(emptyList(), 0))
    override suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit> {
        messagesById[emailId]?.let { messagesById[emailId] = it.copy(folderId = folderId) }
        return Result.success(Unit)
    }

    override suspend fun bulkUpdate(
        mailboxId: String,
        action: BulkAction,
        emailIds: Collection<String>,
        folderId: String?,
        labelId: String?,
    ): BulkOutcome {
        for (id in emailIds) {
            when (action) {
                BulkAction.READ -> setRead(mailboxId, id, true)
                BulkAction.UNREAD -> setRead(mailboxId, id, false)
                BulkAction.STAR -> setStarred(mailboxId, id, true)
                BulkAction.UNSTAR -> setStarred(mailboxId, id, false)
                BulkAction.MOVE -> moveEmail(mailboxId, id, folderId ?: SystemFolders.INBOX)
                BulkAction.DELETE -> deleteEmail(mailboxId, id)
                BulkAction.LABEL, BulkAction.UNLABEL -> Unit // demo mail carries no labels
            }
        }
        return BulkOutcome(succeeded = emailIds.toSet(), failed = emptySet())
    }

    override suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit> {
        messagesById[emailId]?.let { messagesById[emailId] = it.copy(folderId = SystemFolders.TRASH) }
        return Result.success(Unit)
    }

    override suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit> {
        messagesById[emailId]?.let { messagesById[emailId] = it.copy(read = read) }
        return Result.success(Unit)
    }

    override suspend fun setStarred(mailboxId: String, emailId: String, starred: Boolean): Result<Unit> {
        messagesById[emailId]?.let { messagesById[emailId] = it.copy(starred = starred) }
        return Result.success(Unit)
    }

    override suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit> {
        messagesById.values.filter { it.threadId == threadId }.forEach { messagesById[it.id] = it.copy(read = true) }
        return Result.success(Unit)
    }

    override suspend fun sendEmail(mailboxId: String, request: ComposeEmailRequest): Result<Unit> = Result.success(Unit)
    override suspend fun replyEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> = Result.success(Unit)
    override suspend fun forwardEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> = Result.success(Unit)
    override suspend fun clearCache() { /* nothing cached */ }
}

class DemoThreadRepository : ThreadRepository {
    override suspend fun getThread(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>> {
        val details = if (threadId != null) {
            messagesById.values.filter { it.threadId == threadId }.sortedByDescending { it.ageAgo }.map { it.toDetail() }
        } else {
            listOfNotNull(messagesById[emailId]?.toDetail())
        }
        return Result.success(details)
    }
}

class DemoDraftRepository : DraftRepository {
    override suspend fun save(draft: Draft) {
        demoDrafts.update { list -> list.filterNot { it.id == draft.id } + draft }
    }
    override suspend fun get(id: String): Draft? = demoDrafts.value.find { it.id == id }
    override fun observe(mailboxId: String): Flow<List<Draft>> =
        demoDrafts.map { list -> list.filter { it.mailboxId == mailboxId }.sortedByDescending { it.updatedAt } }
    override suspend fun delete(id: String) {
        demoDrafts.update { list -> list.filterNot { it.id == id } }
    }
}

class DemoAttachmentRepository : AttachmentRepository {
    override suspend fun download(mailboxId: String, emailId: String, attachment: EmailAttachment): Result<String> =
        Result.failure(UnsupportedOperationException("Attachments aren't downloadable in demo mode"))
}

val demoRepositoryModule = module {
    single<CredentialsRepository> { DemoCredentialsRepository() }
    single<ConnectionRepository> { DemoConnectionRepository() }
    single<MailboxRepository> { DemoMailboxRepository() }
    single<EmailRepository> { DemoEmailRepository() }
    single<FolderRepository> { DemoFolderRepository() }
    single<LabelRepository> { DemoLabelRepository() }
    single<ScheduleRepository> { DemoScheduleRepository() }
    single<ContactRepository> { DemoContactRepository() }
    single<ThreadRepository> { DemoThreadRepository() }
    single<DraftRepository> { DemoDraftRepository() }
    single<AttachmentRepository> { DemoAttachmentRepository() }
}

/** In-memory labels for demo mode. */
class DemoLabelRepository : LabelRepository {
    private val labels = mutableListOf(
        Label("work", "Work", "blue"),
        Label("travel", "Travel", "teal"),
    )

    override suspend fun getLabels(mailboxId: String): Result<List<Label>> = Result.success(labels.toList())

    override suspend fun createLabel(mailboxId: String, name: String, color: String): Result<Label> {
        val label = Label(id = name.lowercase().replace(' ', '-'), name = name, color = color)
        labels += label
        return Result.success(label)
    }

    override suspend fun updateLabel(mailboxId: String, labelId: String, name: String?, color: String?): Result<Label> {
        val index = labels.indexOfFirst { it.id == labelId }
        if (index < 0) return Result.failure(IllegalArgumentException("Label not found"))
        labels[index] = labels[index].copy(name = name ?: labels[index].name, color = color ?: labels[index].color)
        return Result.success(labels[index])
    }

    override suspend fun deleteLabel(mailboxId: String, labelId: String): Result<Unit> {
        labels.removeAll { it.id == labelId }
        return Result.success(Unit)
    }
}

/** In-memory scheduled sends for demo mode. */
class DemoScheduleRepository : ScheduleRepository {
    private val queue = mutableListOf<ScheduledEmail>()

    override suspend fun schedule(
        mailboxId: String,
        request: ComposeEmailRequest,
        kind: ScheduleKind,
        originalEmailId: String?,
        sendAt: Instant,
    ): Result<Unit> {
        queue += ScheduledEmail(
            id = "demo-scheduled-${queue.size}",
            kind = kind,
            sendAt = sendAt.toString(),
            status = ScheduledStatus.PENDING,
            error = null,
            to = request.to.joinToString(", "),
            subject = request.subject,
        )
        return Result.success(Unit)
    }

    override suspend fun getScheduled(mailboxId: String): Result<List<ScheduledEmail>> = Result.success(queue.sortedBy { it.sendAt })

    override suspend fun cancel(mailboxId: String, scheduledId: String): Result<Unit> {
        queue.removeAll { it.id == scheduledId }
        return Result.success(Unit)
    }
}

/** A few made-up correspondents for demo mode's autocomplete. */
class DemoContactRepository : ContactRepository {
    private val contacts = listOf(
        Contact("alex.morgan@example.com", 12),
        Contact("priya.shah@example.com", 8),
        Contact("sam.lee@example.com", 5),
    )

    override suspend fun search(mailboxId: String, query: String, limit: Int): Result<List<Contact>> =
        Result.success(contacts.filter { it.address.contains(query.lowercase()) }.take(limit))
}
