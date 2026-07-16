package com.sonicstarsolutions.agentic.inbox.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val coreModule = module {
    singleOf(::Greeter)
    // KSafe() has no commonMain constructor — each platform's factory (context needs vary)
    // is registered by that platform's own module (see androidMain/iosMain PlatformModule.kt).
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
