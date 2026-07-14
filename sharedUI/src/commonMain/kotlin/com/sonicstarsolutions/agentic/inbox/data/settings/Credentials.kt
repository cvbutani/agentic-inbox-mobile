package com.sonicstarsolutions.agentic.inbox.data.settings

/**
 * Plaintext Access service-token credentials for the agentic-inbox Worker.
 * Option A from the plan; storage backend [CredentialsRepository] can be swapped
 * to EncryptedSharedPreferences / Keychain in a follow-up — see plan §3.
 */
data class Credentials(
    val baseUrl: String,
    val clientId: String,
    val clientSecret: String,
) {
    fun isComplete(): Boolean =
        baseUrl.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()
}
