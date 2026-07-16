package com.sonicstarsolutions.agentic.inbox.di

import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for the Ktorfit baseUrl / HttpClient.defaultRequest interaction in
 * NetworkModule.kt.
 *
 * Ktorfit's generated code always does `takeFrom(ktorfitBaseUrl + path)`. If ktorfitBaseUrl is a
 * real absolute URL (even a placeholder one), the concatenated string parses with a non-empty
 * host, and Ktor's DefaultRequest.mergeUrls() skips applying the dynamic host from
 * HttpClient.defaultRequest whenever the request already has one — meaning the real Worker URL
 * from CredentialsRepository would silently never be used. Ktorfit's baseUrl must stay "" so its
 * generated calls parse as bare relative paths (empty host) and defaultRequest's merge actually
 * applies.
 */
class NetworkModuleBaseUrlTest {

    @Test
    fun `empty ktorfit baseUrl leaves host empty so defaultRequest can inject it`() {
        val ktorfitBaseUrl = ""
        val url = URLBuilder().apply { takeFrom(ktorfitBaseUrl + "/api/v1/config") }.build()

        assertTrue(url.host.isEmpty(), "host should stay empty for defaultRequest to fill in")
        assertEquals("/api/v1/config", url.encodedPath)
    }

    @Test
    fun `absolute placeholder ktorfit baseUrl sets a host which is the bug`() {
        val ktorfitBaseUrl = "https://placeholder.invalid/"
        val url = URLBuilder().apply { takeFrom(ktorfitBaseUrl + "/api/v1/config") }.build()

        // Documents why an absolute placeholder is wrong: a non-empty host here means
        // DefaultRequest.mergeUrls() would skip injecting the real credentials-based URL.
        assertEquals("placeholder.invalid", url.host)
    }
}
