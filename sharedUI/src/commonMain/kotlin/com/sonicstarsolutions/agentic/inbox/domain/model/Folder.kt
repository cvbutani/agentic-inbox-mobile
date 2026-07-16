package com.sonicstarsolutions.agentic.inbox.domain.model

data class Folder(
    val id: String,
    val name: String,
    val unreadCount: Int = 0,
    val isSystem: Boolean = false,
)

/**
 * System folder ids, from the Worker's shared/folders.ts — always present, never user-editable,
 * and always shown in the drawer ahead of any custom folder. Server-reported unread counts get
 * merged onto these defaults by FolderRepositoryImpl; the id/name/order here is the fallback.
 */
object SystemFolders {
    const val INBOX = "inbox"
    const val DRAFT = "draft"
    const val SENT = "sent"
    const val ARCHIVE = "archive"
    const val SPAM = "spam"
    const val TRASH = "trash"

    val defaults: List<Folder> = listOf(
        Folder(id = INBOX, name = "Inbox", isSystem = true),
        Folder(id = DRAFT, name = "Drafts", isSystem = true),
        Folder(id = SENT, name = "Sent", isSystem = true),
        Folder(id = ARCHIVE, name = "Archive", isSystem = true),
        Folder(id = SPAM, name = "Junk", isSystem = true),
        Folder(id = TRASH, name = "Trash", isSystem = true),
    )

    val ids: Set<String> = defaults.map { it.id }.toSet()
}
