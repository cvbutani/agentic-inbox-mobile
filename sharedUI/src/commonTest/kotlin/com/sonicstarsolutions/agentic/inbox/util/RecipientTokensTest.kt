package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipientTokensTest {

    @Test
    fun `the token being typed is whatever follows the last separator`() {
        assertEquals("bo", RecipientTokens.current("alice@x.dev, bo"))
        assertEquals("bo", RecipientTokens.current("alice@x.dev; bo"))
        assertEquals("bo", RecipientTokens.current("bo"))
        assertEquals("a b", RecipientTokens.current("x@y.dev,   a b"))
    }

    @Test
    fun `right after a separator there is nothing being typed`() {
        assertEquals("", RecipientTokens.current(""))
        assertEquals("", RecipientTokens.current("alice@x.dev,"))
        assertEquals("", RecipientTokens.current("alice@x.dev, "))
    }

    @Test
    fun `a named address in progress is still the last token`() {
        assertEquals("Bob <bo", RecipientTokens.current("Alice <alice@x.dev>, Bob <bo"))
    }

    @Test
    fun `picking a suggestion replaces only the token being typed and leaves a separator`() {
        assertEquals("bob@x.dev, ", RecipientTokens.replaceCurrent("bo", "bob@x.dev"))
        assertEquals("alice@x.dev, bob@x.dev, ", RecipientTokens.replaceCurrent("alice@x.dev, bo", "bob@x.dev"))
        assertEquals("a@x.dev; bob@x.dev, ", RecipientTokens.replaceCurrent("a@x.dev; bo", "bob@x.dev"))
    }

    @Test
    fun `picking with nothing typed yet just appends`() {
        assertEquals("alice@x.dev, bob@x.dev, ", RecipientTokens.replaceCurrent("alice@x.dev, ", "bob@x.dev"))
        assertEquals("bob@x.dev, ", RecipientTokens.replaceCurrent("", "bob@x.dev"))
    }
}
