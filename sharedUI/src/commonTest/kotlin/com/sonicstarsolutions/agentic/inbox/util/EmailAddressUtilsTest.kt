package com.sonicstarsolutions.agentic.inbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class EmailAddressUtilsTest {

    @Test
    fun `extractAddress pulls the address out of a Name less-than email greater-than string`() {
        assertEquals("alice@example.dev", EmailAddressUtils.extractAddress("Alice <alice@example.dev>"))
    }

    @Test
    fun `extractAddress returns a plain address unchanged`() {
        assertEquals("alice@example.dev", EmailAddressUtils.extractAddress("alice@example.dev"))
    }

    @Test
    fun `extractAddress trims surrounding whitespace`() {
        assertEquals("alice@example.dev", EmailAddressUtils.extractAddress("  alice@example.dev  "))
        assertEquals("alice@example.dev", EmailAddressUtils.extractAddress("  Alice <alice@example.dev>  "))
    }

    @Test
    fun `parseAddressList splits on commas and semicolons and trims each entry`() {
        assertEquals(
            listOf("a@example.dev", "b@example.dev", "c@example.dev"),
            EmailAddressUtils.parseAddressList("a@example.dev, b@example.dev; c@example.dev"),
        )
    }

    @Test
    fun `parseAddressList extracts addresses from Name less-than email greater-than entries`() {
        assertEquals(
            listOf("a@example.dev", "b@example.dev"),
            EmailAddressUtils.parseAddressList("Alice <a@example.dev>, Bob <b@example.dev>"),
        )
    }

    @Test
    fun `parseAddressList filters out blank entries`() {
        assertEquals(
            listOf("a@example.dev"),
            EmailAddressUtils.parseAddressList("a@example.dev, , ;  "),
        )
    }

    @Test
    fun `parseAddressList on a blank string returns an empty list`() {
        assertEquals(emptyList(), EmailAddressUtils.parseAddressList("   "))
    }
}
