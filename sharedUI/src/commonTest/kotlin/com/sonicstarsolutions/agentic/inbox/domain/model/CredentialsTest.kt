package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsTest {

    @Test
    fun `scheme-less base url gets https prepended`() {
        val credentials = Credentials(baseUrl = "my-worker.example.dev")
        assertEquals("https://my-worker.example.dev", credentials.normalizedBaseUrl())
    }

    @Test
    fun `existing https scheme is kept`() {
        val credentials = Credentials(baseUrl = "https://my-worker.example.dev")
        assertEquals("https://my-worker.example.dev", credentials.normalizedBaseUrl())
    }

    @Test
    fun `existing http scheme is kept`() {
        val credentials = Credentials(baseUrl = "http://localhost:8787")
        assertEquals("http://localhost:8787", credentials.normalizedBaseUrl())
    }

    @Test
    fun `trailing slash and whitespace are stripped`() {
        val credentials = Credentials(baseUrl = "  https://my-worker.example.dev/  ")
        assertEquals("https://my-worker.example.dev", credentials.normalizedBaseUrl())
    }

    @Test
    fun `blank base url normalizes to empty`() {
        assertEquals("", Credentials(baseUrl = "   ").normalizedBaseUrl())
    }
}
