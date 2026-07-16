package com.sonicstarsolutions.agentic.inbox.di

import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

val platformSettingsDataStoreModule = module {
    // Engine only — the HttpClient itself (defaultRequest URL/headers, ContentNegotiation) is
    // built in commonMain's networkModule. Providing a full HttpClient here would silently
    // override that configured client (platform modules load last), which is exactly the bug
    // that sent every request to localhost.
    single<HttpClientEngine> { Darwin.create() }
    single { KSafe() }
}
