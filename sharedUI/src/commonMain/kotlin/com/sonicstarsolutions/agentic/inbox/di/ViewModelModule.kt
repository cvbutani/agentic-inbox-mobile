package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.ui.compose.ComposeMode
import com.sonicstarsolutions.agentic.inbox.ui.compose.ComposeViewModel
import com.sonicstarsolutions.agentic.inbox.ui.inbox.InboxViewModel
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker.MailboxPickerViewModel
import com.sonicstarsolutions.agentic.inbox.ui.onboarding.OnboardingViewModel
import com.sonicstarsolutions.agentic.inbox.ui.search.SearchViewModel
import com.sonicstarsolutions.agentic.inbox.ui.splash.SplashViewModel
import com.sonicstarsolutions.agentic.inbox.ui.thread.ThreadViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::MailboxPickerViewModel)
    // Runtime args (mailboxId/mailboxName) come from the Inbox nav key via parametersOf(...);
    // the use case is injected. Destructure the ParametersHolder positionally.
    viewModel { (mailboxId: String, mailboxName: String) ->
        InboxViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), mailboxId, mailboxName)
    }
    viewModel { (mailboxId: String, emailId: String, threadId: String?) ->
        ThreadViewModel(get(), get(), get(), get(), get(), get(), get(), mailboxId, emailId, threadId)
    }
    viewModel { (mailboxId: String, mode: ComposeMode, emailId: String?, threadId: String?) ->
        ComposeViewModel(get(), get(), get(), get(), get(), mailboxId, mode, emailId, threadId)
    }
    viewModel { (mailboxId: String) ->
        SearchViewModel(get(), mailboxId)
    }
}
