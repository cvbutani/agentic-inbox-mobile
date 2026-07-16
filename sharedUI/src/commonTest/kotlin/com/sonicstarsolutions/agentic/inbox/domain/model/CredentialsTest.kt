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
}
