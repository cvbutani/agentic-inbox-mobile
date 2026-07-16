package com.sonicstarsolutions.agentic.inbox.di

import android.content.Context
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

fun platformModule(context: Context) = module {
    // Engine only — the HttpClient itself (defaultRequest URL/headers, ContentNegotiation) is
    // built in commonMain's networkModule. Providing a full HttpClient here would silently
    // override that configured client (platform modules load last), which is exactly the bug
    // that sent every request to localhost.
    single<HttpClientEngine> { OkHttp.create() }
    single { KSafe(context) }
}
