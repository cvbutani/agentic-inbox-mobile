package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `isComplete is true when all fields are non-blank`() {
        val credentials = Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "secret")
        assertTrue(credentials.isComplete())
    }

    @Test
    fun `isComplete is false when base url is blank`() {
        val credentials = Credentials(baseUrl = "  ", clientId = "id", clientSecret = "secret")
        assertFalse(credentials.isComplete())
    }

    @Test
    fun `isComplete is false when client id is blank`() {
        val credentials = Credentials(baseUrl = "my-worker.example.dev", clientId = "", clientSecret = "secret")
        assertFalse(credentials.isComplete())
    }

    @Test
    fun `isComplete is false when client secret is blank`() {
        val credentials = Credentials(baseUrl = "my-worker.example.dev", clientId = "id", clientSecret = "  ")
        assertFalse(credentials.isComplete())
    }

    @Test
    fun `isComplete is false for the default empty instance`() {
        assertFalse(Credentials().isComplete())
    }

    @Test
    fun `displayHost drops the scheme and trailing slash`() {
        assertEquals("my-worker.example.dev", Credentials(baseUrl = "https://my-worker.example.dev/").displayHost())
    }

    @Test
    fun `displayHost drops any path`() {
        assertEquals("my-worker.example.dev", Credentials(baseUrl = "https://my-worker.example.dev/api/v1").displayHost())
    }

    @Test
    fun `displayHost keeps a port`() {
        assertEquals("localhost:8787", Credentials(baseUrl = "http://localhost:8787").displayHost())
    }

    @Test
    fun `displayHost accepts a scheme-less url`() {
        assertEquals("my-worker.example.dev", Credentials(baseUrl = "  my-worker.example.dev ").displayHost())
    }

    @Test
    fun `displayHost is empty for a blank url`() {
        assertEquals("", Credentials(baseUrl = "  ").displayHost())
    }
}
