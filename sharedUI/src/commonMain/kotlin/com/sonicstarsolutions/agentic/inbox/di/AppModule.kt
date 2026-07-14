package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.KtorAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.buildHttpClient
import com.sonicstarsolutions.agentic.inbox.data.network.httpPlatformEngine
import com.sonicstarsolutions.agentic.inbox.data.settings.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.data.settings.platformSettings
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker.MailboxPickerViewModel
import com.sonicstarsolutions.agentic.inbox.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val coreModule = module {
    singleOf(::Greeter)
}

private val settingsModule = module {
    single { CredentialsRepository(platformSettings()) }
}

private val networkModule = module {
    single { buildHttpClient(engine = httpPlatformEngine(), credentials = get()) }
    single<AgenticInboxApi> { KtorAgenticInboxApi(get()) }
}

private val uiModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::MailboxPickerViewModel)
}


/**
 * Root Koin module aggregator. Add real feature/data layer modules to
 * [allModules] as the app grows; do not declare bindings directly here.
 */
val allModules = listOf(
    coreModule,
    settingsModule,
    networkModule,
    uiModule,
)


class Greeter {
    fun greeting(): String = "Agentic Inbox — Koin is wired up"
}
