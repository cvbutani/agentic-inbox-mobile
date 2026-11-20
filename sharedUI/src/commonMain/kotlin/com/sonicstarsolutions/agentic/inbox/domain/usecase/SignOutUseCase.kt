package com.sonicstarsolutions.agentic.inbox.domain.usecase

/**
 * Signs the user out of this device: forgets the stored credentials and wipes the local mail
 * cache, so a different account signing in next never sees the previous account's mail through
 * the offline fallback. Drafts are kept (see [ClearLocalCacheUseCase]).
 */
class SignOutUseCase(
    private val clearCredentials: ClearCredentialsUseCase,
    private val clearLocalCache: ClearLocalCacheUseCase,
) {
    suspend operator fun invoke() {
        clearCredentials()
        clearLocalCache()
    }
}
