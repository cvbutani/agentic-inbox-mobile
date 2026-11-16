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

    @Test
    fun `displayName returns the parsed name for a Name less-than email greater-than string`() {
        assertEquals(
            "Alice",
            EmailAddressUtils.displayName("Alice <alice@example.dev>", ownEmail = "me@example.dev"),
        )
    }

    @Test
    fun `displayName returns a plain address unchanged when there is no bracketed name`() {
        assertEquals(
            "alice@example.dev",
            EmailAddressUtils.displayName("alice@example.dev", ownEmail = "me@example.dev"),
        )
    }

    @Test
    fun `displayName returns You when the address matches the owner's email`() {
        assertEquals(
            "You",
            EmailAddressUtils.displayName("Me <me@example.dev>", ownEmail = "me@example.dev"),
        )
    }

    @Test
    fun `displayName matching is case-insensitive`() {
        assertEquals(
            "You",
            EmailAddressUtils.displayName("Me <ME@Example.dev>", ownEmail = "me@example.dev"),
        )
    }

    @Test
    fun `displayName does not say You when ownEmail is null`() {
        assertEquals(
            "Me",
            EmailAddressUtils.displayName("Me <me@example.dev>", ownEmail = null),
        )
    }

    @Test
    fun `displayName does not say You for a different address`() {
        assertEquals(
            "Alice",
            EmailAddressUtils.displayName("Alice <alice@example.dev>", ownEmail = "me@example.dev"),
        )
    }

    // --- parseAddresses: one entry per person, keeping the name so it can be shown and searched ---

    @Test
    fun `parseAddresses keeps the name and the address of each entry`() {
        assertEquals(
            listOf(
                EmailAddressUtils.Person(name = "Alice", address = "a@example.dev"),
                EmailAddressUtils.Person(name = null, address = "b@example.dev"),
            ),
            EmailAddressUtils.parseAddresses("Alice <a@example.dev>, b@example.dev"),
        )
    }

    @Test
    fun `parseAddresses does not split a quoted name on its comma`() {
        assertEquals(
            listOf(
                EmailAddressUtils.Person(name = "Lee, Dave", address = "dave@example.dev"),
                EmailAddressUtils.Person(name = null, address = "x@example.dev"),
            ),
            EmailAddressUtils.parseAddresses("\"Lee, Dave\" <dave@example.dev>; x@example.dev"),
        )
    }

    @Test
    fun `parseAddresses on blank input is empty`() {
        assertEquals(emptyList(), EmailAddressUtils.parseAddresses(null))
        assertEquals(emptyList(), EmailAddressUtils.parseAddresses("  , ; "))
    }

    @Test
    fun `a person is labelled by name when known and by address otherwise`() {
        assertEquals("Alice", EmailAddressUtils.Person("Alice", "a@example.dev").label)
        assertEquals("a@example.dev", EmailAddressUtils.Person(null, "a@example.dev").label)
    }

    // --- withName: folding the server's separate sender_name back into one header string ---

    @Test
    fun `withName builds a Name angle-address header`() {
        assertEquals("Dave <dave@example.dev>", EmailAddressUtils.withName("dave@example.dev", "Dave"))
    }

    @Test
    fun `withName quotes a name that has a comma`() {
        assertEquals("\"Lee, Dave\" <dave@example.dev>", EmailAddressUtils.withName("dave@example.dev", "Lee, Dave"))
    }

    @Test
    fun `withName leaves the address alone when there is no name`() {
        assertEquals("dave@example.dev", EmailAddressUtils.withName("dave@example.dev", null))
        assertEquals("dave@example.dev", EmailAddressUtils.withName("dave@example.dev", "  "))
    }

    @Test
    fun `withName does not double up a sender that already carries a name`() {
        assertEquals("Dave <dave@example.dev>", EmailAddressUtils.withName("Dave <dave@example.dev>", "Other"))
    }

    @Test
    fun `displayName strips the quotes round a quoted name`() {
        assertEquals("Lee, Dave", EmailAddressUtils.displayName("\"Lee, Dave\" <dave@example.dev>", ownEmail = null))
    }

    // --- senderLabel: what the From line shows ---

    @Test
    fun `senderLabel shows the name of your own message when there is one`() {
        assertEquals("Me Myself", EmailAddressUtils.senderLabel("Me Myself <me@example.dev>", ownEmail = "me@example.dev"))
    }

    @Test
    fun `senderLabel falls back to You for your own bare address`() {
        assertEquals("You", EmailAddressUtils.senderLabel("me@example.dev", ownEmail = "me@example.dev"))
    }

    @Test
    fun `senderLabel shows other people by name or address`() {
        assertEquals("Alice", EmailAddressUtils.senderLabel("Alice <a@example.dev>", ownEmail = "me@example.dev"))
        assertEquals("a@example.dev", EmailAddressUtils.senderLabel("a@example.dev", ownEmail = "me@example.dev"))
    }
}
