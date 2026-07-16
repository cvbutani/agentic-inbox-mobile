package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetAllowedDomainsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailReadUseCase
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
    singleOf(::CreateMailboxUseCase)
    singleOf(::GetAllowedDomainsUseCase)
    singleOf(::GetMailboxUseCase)
    singleOf(::GetEmailsUseCase)
    singleOf(::GetFoldersUseCase)
    singleOf(::CreateFolderUseCase)
    singleOf(::GetThreadUseCase)
    singleOf(::MoveEmailUseCase)
    singleOf(::DeleteEmailUseCase)
    singleOf(::SetEmailReadUseCase)
    singleOf(::MarkThreadReadUseCase)
    singleOf(::SendEmailUseCase)
    singleOf(::ReplyEmailUseCase)
    singleOf(::ForwardEmailUseCase)
}
