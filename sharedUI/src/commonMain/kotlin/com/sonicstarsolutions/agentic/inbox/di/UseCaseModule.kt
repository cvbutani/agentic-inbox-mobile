package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ValidateConnectionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::ObserveCredentialsUseCase)
    singleOf(::SaveCredentialsUseCase)
    singleOf(::ClearCredentialsUseCase)
    singleOf(::LoadCredentialsUseCase)
    singleOf(::ValidateConnectionUseCase)
    singleOf(::GetMailboxesUseCase)
    singleOf(::GetEmailsUseCase)
}
