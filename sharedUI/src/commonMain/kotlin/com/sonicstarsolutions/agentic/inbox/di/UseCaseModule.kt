package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.domain.usecase.BulkUpdateEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CancelScheduledEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetScheduledEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveLiveUpdatesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ScheduleEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchContactsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsByLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetLabelsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateLabelUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearLocalCacheUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SignOutUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetAllowedDomainsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDomainEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchDomainEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDomainFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveDomainLiveUpdatesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DownloadAttachmentUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.LoadCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDomainSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateDomainSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveDraftsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RenameFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendDraftEmailUseCase
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
    singleOf(::SignOutUseCase)
    singleOf(::LoadCredentialsUseCase)
    singleOf(::ValidateConnectionUseCase)
    singleOf(::GetMailboxesUseCase)
    singleOf(::CreateMailboxUseCase)
    singleOf(::DeleteMailboxUseCase)
    singleOf(::GetAllowedDomainsUseCase)
    singleOf(::GetMailboxUseCase)
    singleOf(::GetEmailsUseCase)
    singleOf(::GetDomainEmailsUseCase)
    singleOf(::SearchDomainEmailsUseCase)
    singleOf(::GetDomainFoldersUseCase)
    singleOf(::ObserveDomainLiveUpdatesUseCase)
    singleOf(::SearchEmailsUseCase)
    singleOf(::GetFoldersUseCase)
    singleOf(::CreateFolderUseCase)
    singleOf(::RenameFolderUseCase)
    singleOf(::DeleteFolderUseCase)
    singleOf(::GetThreadUseCase)
    singleOf(::DownloadAttachmentUseCase)
    singleOf(::MoveEmailUseCase)
    singleOf(::BulkUpdateEmailsUseCase)
    singleOf(::ScheduleEmailUseCase)
    singleOf(::SearchContactsUseCase)
    singleOf(::ObserveLiveUpdatesUseCase)
    singleOf(::GetScheduledEmailsUseCase)
    singleOf(::CancelScheduledEmailUseCase)
    singleOf(::GetEmailsByLabelUseCase)
    singleOf(::GetLabelsUseCase)
    singleOf(::CreateLabelUseCase)
    singleOf(::UpdateLabelUseCase)
    singleOf(::DeleteLabelUseCase)
    singleOf(::DeleteEmailUseCase)
    singleOf(::SetEmailReadUseCase)
    singleOf(::SetEmailStarredUseCase)
    singleOf(::MarkThreadReadUseCase)
    singleOf(::SendEmailUseCase)
    singleOf(::ReplyEmailUseCase)
    singleOf(::ForwardEmailUseCase)
    singleOf(::SendDraftEmailUseCase)
    singleOf(::SaveDraftUseCase)
    singleOf(::GetDraftUseCase)
    singleOf(::ObserveDraftsUseCase)
    singleOf(::DeleteDraftUseCase)
    singleOf(::GetMailboxSettingsUseCase)
    singleOf(::GetDomainSettingsUseCase)
    singleOf(::UpdateDomainSettingsUseCase)
    singleOf(::UpdateMailboxSettingsUseCase)
}
