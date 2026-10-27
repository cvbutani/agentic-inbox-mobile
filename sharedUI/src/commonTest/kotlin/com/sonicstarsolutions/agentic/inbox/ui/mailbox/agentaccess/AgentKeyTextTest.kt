package com.sonicstarsolutions.agentic.inbox.ui.mailbox.agentaccess

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.testutil.agentKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentKeyTextTest {

    @Test
    fun `lists the permissions in order`() {
        val key = agentKey(scopes = listOf(AgentScope.READ, AgentScope.DRAFT))

        assertEquals("Read · Draft", agentKeySummary(key))
    }

    @Test
    fun `a sending key mentions its daily limit`() {
        val key = agentKey(scopes = listOf(AgentScope.READ, AgentScope.SEND), dailySendLimit = 20)

        assertEquals("Read · Send · up to 20 sends a day", agentKeySummary(key))
    }

    @Test
    fun `a limit of one is singular`() {
        val key = agentKey(scopes = listOf(AgentScope.SEND), dailySendLimit = 1)

        assertEquals("Send · up to 1 send a day", agentKeySummary(key))
    }

    @Test
    fun `a sending key with no limit says nothing about one`() {
        val key = agentKey(scopes = listOf(AgentScope.SEND), dailySendLimit = null)

        assertEquals("Send", agentKeySummary(key))
    }

    @Test
    fun `a limit on a key that cannot send is not mentioned`() {
        val key = agentKey(scopes = listOf(AgentScope.READ), dailySendLimit = 5)

        assertEquals("Read", agentKeySummary(key))
    }

    @Test
    fun `a key whose scopes were all unknown to this app still has a summary`() {
        assertEquals("No permissions this app recognises", agentKeySummary(agentKey(scopes = emptyList())))
    }
}
