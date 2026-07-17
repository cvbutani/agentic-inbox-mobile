package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

/**
 * Wipes every locally cached mailbox/folder/email row — the offline-read mirror each repository
 * keeps in Room, not the drafts table. Drafts are deliberately excluded: they're the only copy of
 * that data (see [com.sonicstarsolutions.agentic.inbox.domain.model.Draft]), not a cache of
 * something the server can re-supply, so signing out must not delete unsent work.
 *
 * Used on sign-out so a different account signing in on this device never sees the previous
 * account's cached mail through the offline-fallback path in getEmails/getFolders/getMailboxes.
 */
class ClearLocalCacheUseCase(
    private val mailboxRepository: MailboxRepository,
    private val folderRepository: FolderRepository,
    private val emailRepository: EmailRepository,
) {
    suspend operator fun invoke() {
        mailboxRepository.clearCache()
        folderRepository.clearCache()
        emailRepository.clearCache()
    }
}
