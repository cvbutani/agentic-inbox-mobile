package com.sonicstarsolutions.agentic.inbox.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val coreModule = module {
    singleOf(::Greeter)
    // KSafe() has no commonMain constructor — each platform's factory (context needs vary)
    // is registered by that platform's own module (see androidMain/iosMain PlatformModule.kt).

    // Application-lifetime scope for work that must finish even as the screen that started it
    // goes away — currently just the composer's draft autosave (see ComposeViewModel). Not tied
    // to any ViewModel, and never cancelled. SupervisorJob so one failure can't take down the
    // rest of the app's background work.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}

val commonModules = listOf(
    coreModule,
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
)

class Greeter {
    fun greeting(): String = "Agentic Inbox — Koin is wired up"
}
