package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearLocalCacheUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetAllowedDomainsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveDraftsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RenameFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.StageCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ValidateConnectionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::ObserveCredentialsUseCase)
    singleOf(::SaveCredentialsUseCase)
    singleOf(::StageCredentialsUseCase)
    singleOf(::ClearCredentialsUseCase)
    singleOf(::ClearLocalCacheUseCase)
    singleOf(::LoadCredentialsUseCase)
    singleOf(::ValidateConnectionUseCase)
    singleOf(::GetMailboxesUseCase)
    singleOf(::CreateMailboxUseCase)
    singleOf(::DeleteMailboxUseCase)
    singleOf(::GetAllowedDomainsUseCase)
    singleOf(::GetMailboxUseCase)
    singleOf(::GetEmailsUseCase)
    singleOf(::SearchEmailsUseCase)
    singleOf(::GetFoldersUseCase)
    singleOf(::CreateFolderUseCase)
    singleOf(::RenameFolderUseCase)
    singleOf(::DeleteFolderUseCase)
    singleOf(::GetThreadUseCase)
    singleOf(::MoveEmailUseCase)
    singleOf(::DeleteEmailUseCase)
    singleOf(::SetEmailReadUseCase)
    singleOf(::SetEmailStarredUseCase)
    singleOf(::MarkThreadReadUseCase)
    singleOf(::SendEmailUseCase)
    singleOf(::ReplyEmailUseCase)
    singleOf(::ForwardEmailUseCase)
    singleOf(::SaveDraftUseCase)
    singleOf(::GetDraftUseCase)
    singleOf(::ObserveDraftsUseCase)
    singleOf(::DeleteDraftUseCase)
}
