package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailboxSettingsMappingTest {

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    // --- parsing ---------------------------------------------------------------------------

    @Test
    fun `missing settings parse to all-off blank defaults`() {
        assertEquals(MailboxSettings(), (null as JsonObject?).toMailboxSettings())
        assertEquals(MailboxSettings(), obj("{}").toMailboxSettings())
    }

    @Test
    fun `every editable field is read from the worker's settings object`() {
        val settings = obj(
            """
            {"fromName":"Alice","signature":{"enabled":true,"text":"Cheers","html":"<b>Cheers</b>"},
             "autoReply":{"enabled":true,"subject":"Away","message":"Back Monday"}}
            """.trimIndent(),
        ).toMailboxSettings()

        assertEquals(
            MailboxSettings(
                fromName = "Alice",
                signatureEnabled = true,
                signatureText = "Cheers",
            ),
            settings,
        )
    }

    @Test
    fun `wrongly typed values fall back to defaults instead of throwing`() {
        val settings = obj("""{"fromName":5,"signature":"nope","autoReply":{"enabled":"yes","message":null}}""").toMailboxSettings()

        assertEquals(MailboxSettings(), settings)
    }

    // --- merging -----------------------------------------------------------------------------

    @Test
    fun `merge keeps keys the app does not edit, at the top level and nested`() {
        val existing = obj(
            """
            {"fromName":"Old","forwarding":{"enabled":true,"email":"x@example.dev"},"autoDraft":{"enabled":false},
             "agentSystemPrompt":"be nice",
             "signature":{"enabled":false,"text":"","html":"","futureKey":7},
             "autoReply":{"enabled":false,"subject":"","message":"","futureKey":"keep"}}
            """.trimIndent(),
        )

        val merged = mergeMailboxSettings(existing, MailboxSettings(fromName = "New"))

        assertEquals("New", merged["fromName"]!!.jsonPrimitive.content)
        assertEquals("x@example.dev", merged["forwarding"]!!.jsonObject["email"]!!.jsonPrimitive.content)
        assertEquals(false, merged["autoDraft"]!!.jsonObject["enabled"]!!.jsonPrimitive.boolean)
        assertEquals("be nice", merged["agentSystemPrompt"]!!.jsonPrimitive.content)
        assertEquals("7", merged["signature"]!!.jsonObject["futureKey"]!!.jsonPrimitive.content)
        assertEquals("keep", merged["autoReply"]!!.jsonObject["futureKey"]!!.jsonPrimitive.content)
    }

    @Test
    fun `merge writes the edited values`() {
        val merged = mergeMailboxSettings(
            obj("{}"),
            MailboxSettings(
                fromName = "Alice",
                signatureEnabled = true,
                signatureText = "Cheers",
            ),
        )

        assertEquals(MailboxSettings("Alice", true, "Cheers"), merged.toMailboxSettings())
    }

    @Test
    fun `merge into missing settings still produces a complete object`() {
        val merged = mergeMailboxSettings(null, MailboxSettings(fromName = "Alice"))

        assertEquals("Alice", merged["fromName"]!!.jsonPrimitive.content)
        assertTrue("signature" in merged)
    }

    // --- signature html stays consistent with the text the web client prefers -------------------

    @Test
    fun `a changed signature text regenerates html so web clients do not show the old one`() {
        val existing = obj("""{"signature":{"enabled":true,"text":"Old","html":"<b>Old</b>"}}""")

        val merged = mergeMailboxSettings(existing, MailboxSettings(signatureEnabled = true, signatureText = "Line one\nLine <two> & more"))

        val signature = merged["signature"]!!.jsonObject
        assertEquals("Line one\nLine <two> & more", signature["text"]!!.jsonPrimitive.content)
        assertEquals("Line one<br>Line &lt;two&gt; &amp; more", signature["html"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an unchanged signature text leaves rich html untouched`() {
        val existing = obj("""{"signature":{"enabled":true,"text":"Cheers","html":"<b>Cheers</b>"}}""")

        val merged = mergeMailboxSettings(existing, MailboxSettings(signatureEnabled = false, signatureText = "Cheers"))

        val signature = merged["signature"]!!.jsonObject
        assertEquals("<b>Cheers</b>", signature["html"]!!.jsonPrimitive.content)
        assertEquals(false, signature["enabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `clearing the signature text clears the html too`() {
        val existing = obj("""{"signature":{"enabled":true,"text":"Old","html":"<b>Old</b>"}}""")

        val merged = mergeMailboxSettings(existing, MailboxSettings(signatureEnabled = true, signatureText = ""))

        assertEquals("", merged["signature"]!!.jsonObject["html"]!!.jsonPrimitive.content)
    }

    // --- aliases ---------------------------------------------------------------------------

    @Test
    fun `aliases are read from the settings and anything that is not text is skipped`() {
        val settings = obj("""{"aliases":["sales@example.dev",5,null,"help@example.dev"]}""").toMailboxSettings()

        assertEquals(listOf("sales@example.dev", "help@example.dev"), settings.aliases)
    }

    @Test
    fun `a wrongly typed aliases value reads as none`() {
        assertEquals(emptyList(), obj("""{"aliases":"sales@example.dev"}""").toMailboxSettings().aliases)
    }

    @Test
    fun `merge writes the aliases as a list and keeps the other keys`() {
        val merged = mergeMailboxSettings(
            obj("""{"fromName":"Old","autoDraft":{"enabled":false}}"""),
            MailboxSettings(fromName = "Old", aliases = listOf("sales@example.dev", "help@example.dev")),
        )

        assertEquals(
            listOf("sales@example.dev", "help@example.dev"),
            merged["aliases"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(false, merged["autoDraft"]!!.jsonObject["enabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `removing every alias writes an empty list so the server drops them`() {
        val merged = mergeMailboxSettings(obj("""{"aliases":["sales@example.dev"]}"""), MailboxSettings(aliases = emptyList()))

        assertEquals(0, merged["aliases"]!!.jsonArray.size)
    }

    // --- retention (auto-delete) -------------------------------------------------------------

    @Test
    fun `retentionDays is read when it is a whole number and null otherwise`() {
        assertEquals(7, obj("""{"retentionDays":7}""").toMailboxSettings().retentionDays)
        assertEquals(0, obj("""{"retentionDays":0}""").toMailboxSettings().retentionDays)
        assertNull(obj("""{}""").toMailboxSettings().retentionDays)
        assertNull(obj("""{"retentionDays":null}""").toMailboxSettings().retentionDays)
        assertNull(obj("""{"retentionDays":"7"}""").toMailboxSettings().retentionDays)
        assertNull(obj("""{"retentionDays":2.5}""").toMailboxSettings().retentionDays)
    }

    @Test
    fun `merge writes a mailbox's own retention`() {
        val merged = mergeMailboxSettings(obj("""{"fromName":"A"}"""), MailboxSettings(fromName = "A", retentionDays = 90))

        assertEquals(90, merged["retentionDays"]?.jsonPrimitive?.int)
    }

    @Test
    fun `merge drops a stored retention when the mailbox goes back to the domain default`() {
        val merged = mergeMailboxSettings(obj("""{"fromName":"A","retentionDays":90}"""), MailboxSettings(fromName = "A", retentionDays = null))

        assertFalse("retentionDays" in merged)
    }
}
