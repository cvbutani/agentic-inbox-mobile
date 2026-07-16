package com.sonicstarsolutions.agentic.inbox.di

import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for the Ktorfit baseUrl / HttpClient.defaultRequest interaction in
 * NetworkModule.kt.
 *
 * Ktorfit's generated code always does `takeFrom(ktorfitBaseUrl + path)`. If ktorfitBaseUrl is a
 * real absolute URL (even a placeholder one), the concatenated string parses with a real, distinct
 * host, and Ktor's DefaultRequest.mergeUrls() skips applying the dynamic host from
 * HttpClient.defaultRequest whenever the request already has one — meaning the real Worker URL
 * from CredentialsRepository would silently never be used. Ktorfit's baseUrl must stay "" so its
 * generated calls parse as bare relative paths — URLBuilder defaults an unset host to
 * "localhost" (not empty; verified against Ktor 3.5.0's URLBuilder), which mergeUrls() still
 * treats as "unset" and overwrites with defaultRequest's host.
 */
class NetworkModuleBaseUrlTest {

    @Test
    fun `empty ktorfit baseUrl leaves the default localhost host for defaultRequest to override`() {
        val ktorfitBaseUrl = ""
        val url = URLBuilder().apply { takeFrom(ktorfitBaseUrl + "/api/v1/config") }.build()

        assertEquals("localhost", url.host, "host should stay at Ktor's default for defaultRequest to overwrite")
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
