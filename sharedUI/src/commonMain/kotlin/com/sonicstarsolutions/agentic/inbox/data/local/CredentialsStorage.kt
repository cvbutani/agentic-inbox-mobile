package com.sonicstarsolutions.agentic.inbox.data.local

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import eu.anifantakis.lib.ksafe.KSafe

/** The persistence seam for [Credentials] — KSafe in production, an in-memory fake in tests
 * (KSafe itself is a platform-constructed library class a common test can't instantiate). */
interface CredentialsStorage {
    suspend fun read(): Credentials
    suspend fun write(credentials: Credentials)
    suspend fun delete()
}

class KSafeCredentialsStorage(private val ksafe: KSafe) : CredentialsStorage {
    companion object {
        private const val KEY_CREDENTIALS = "credentials"
    }

    override suspend fun read(): Credentials = ksafe.get(KEY_CREDENTIALS, Credentials())

    override suspend fun write(credentials: Credentials) = ksafe.put(KEY_CREDENTIALS, credentials)

    override suspend fun delete() = ksafe.delete(KEY_CREDENTIALS)
}
