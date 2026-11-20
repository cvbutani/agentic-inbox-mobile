package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackendInfoTest {

    @Test
    fun `repository link is the fork's https url without a trailing slash`() {
        assertEquals("https://github.com/SonicStar-Solutions/agentic-inbox", BackendInfo.REPO_URL)
        assertFalse(BackendInfo.REPO_URL.endsWith("/"))
    }

    @Test
    fun `requirement line says the backend is required and names it`() {
        assertTrue(BackendInfo.REQUIREMENT.startsWith("Requires"))
        assertTrue(BackendInfo.NAME in BackendInfo.REQUIREMENT)
    }

    @Test
    fun `requirement line does not call the backend a Worker`() {
        // "Worker" is the Cloudflare runtime; the thing the user must deploy is agentic-inbox,
        // and an upstream Worker deployment is exactly the mistake this copy exists to prevent.
        assertFalse("Worker" in BackendInfo.REQUIREMENT)
    }
}
