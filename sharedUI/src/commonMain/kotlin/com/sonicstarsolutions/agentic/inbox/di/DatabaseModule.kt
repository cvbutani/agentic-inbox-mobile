package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.data.local.AppDatabase
import org.koin.dsl.module

// AppDatabase itself is provided by each platform's platformModule (needs an Android Context or
// an iOS file path) — this just exposes the DAOs from whichever instance Koin resolves.
val databaseModule = module {
    single { get<AppDatabase>().mailboxDao() }
    single { get<AppDatabase>().folderDao() }
    single { get<AppDatabase>().emailDao() }
    single { get<AppDatabase>().draftDao() }
}
