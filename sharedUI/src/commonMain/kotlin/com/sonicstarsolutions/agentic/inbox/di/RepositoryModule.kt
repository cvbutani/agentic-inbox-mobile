package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.data.repository.ConnectionRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.CredentialsRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.EmailRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.FolderRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.data.repository.MailboxRepositoryImpl
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import eu.anifantakis.lib.ksafe.KSafe
import org.koin.dsl.module

val repositoryModule = module {
    single<CredentialsRepository> { CredentialsRepositoryImpl(get<KSafe>()) }
    single<MailboxRepository> { MailboxRepositoryImpl(get()) }
    single<EmailRepository> { EmailRepositoryImpl(get()) }
    single<FolderRepository> { FolderRepositoryImpl(get()) }
    single<ConnectionRepository> { ConnectionRepositoryImpl(get()) }
}
