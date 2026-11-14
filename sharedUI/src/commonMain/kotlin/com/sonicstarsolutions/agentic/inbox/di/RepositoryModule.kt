package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.data.local.KSafeCredentialsStorage
import com.sonicstarsolutions.agentic.inbox.data.repository.AttachmentRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.ConnectionRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.CredentialsRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.DraftRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.EmailRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.FolderRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.MailboxRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.ThreadRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.domain.repository.AttachmentRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository
import eu.anifantakis.lib.ksafe.KSafe
import org.koin.dsl.module

val repositoryModule = module {
    single<CredentialsRepository> {
        CredentialsRepositoryImpl(
            storage = KSafeCredentialsStorage(get<KSafe>()),
            // App-lifetime scope: hydration must survive whichever screen happens to be composing.
            scope = get(),
        )
    }
    single<MailboxRepository> { MailboxRepositoryImpl(get(), get()) }
    single<EmailRepository> { EmailRepositoryImpl(get(), get()) }
    single<FolderRepository> { FolderRepositoryImpl(get(), get()) }
    single<ThreadRepository> { ThreadRepositoryImpl(get()) }
    single<DraftRepository> { DraftRepositoryImpl(get()) }
    single<ConnectionRepository> { ConnectionRepositoryImpl(get()) }
    single<AttachmentRepository> { AttachmentRepositoryImpl(get(), get()) }
}
