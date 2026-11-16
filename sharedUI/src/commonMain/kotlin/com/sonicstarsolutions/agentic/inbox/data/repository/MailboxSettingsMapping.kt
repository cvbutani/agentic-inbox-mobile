package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

private fun JsonObject?.string(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun JsonObject?.boolean(key: String): Boolean? = (this?.get(key) as? JsonPrimitive)?.booleanOrNull

/** A whole-number value; a string, fraction or null is not one. */
private fun JsonObject?.int(key: String): Int? = (this?.get(key) as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

private fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

/** The text entries of an array value; anything else in it, or a non-array, is ignored. */
internal fun JsonObject?.stringList(key: String): List<String> =
    (this?.get(key) as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull }
        .orEmpty()

/** Reads the editable fields out of a mailbox's free-form settings. Missing or wrongly typed
 * values become defaults — a settings blob written by another client must never crash the screen. */
internal fun JsonObject?.toMailboxSettings(): MailboxSettings {
    val signature = obj("signature")
    return MailboxSettings(
        fromName = string("fromName").orEmpty(),
        signatureEnabled = signature.boolean("enabled") ?: false,
        signatureText = signature.string("text").orEmpty(),
        aliases = stringList("aliases"),
        retentionDays = int("retentionDays"),
    )
}

/**
 * Applies [edited] on top of [existing]. The Worker's `PUT` replaces the whole settings blob, so
 * every key the app doesn't edit — at the top level (aliases, auto-draft, agent prompt, …) *and* inside `signature` — is
 * carried over as is.
 *
 * The web client renders `signature.html` in preference to `signature.text`, so when the text
 * actually changed the HTML is regenerated from it; otherwise web clients would keep showing the
 * old signature. An unchanged text leaves any rich HTML alone.
 */
internal fun mergeMailboxSettings(existing: JsonObject?, edited: MailboxSettings): JsonObject {
    val current = existing ?: JsonObject(emptyMap())
    val signature = current.obj("signature")

    val textChanged = signature.string("text").orEmpty() != edited.signatureText
    val html = when {
        textChanged -> edited.signatureText.toSignatureHtml()
        else -> signature.string("html").orEmpty()
    }

    val mergedSignature = (signature ?: JsonObject(emptyMap())) + mapOf(
        "enabled" to JsonPrimitive(edited.signatureEnabled),
        "text" to JsonPrimitive(edited.signatureText),
        "html" to JsonPrimitive(html),
    )

    // A null retention means "follow the domain": the key is removed rather than stored as null.
    val withoutRetention = if (edited.retentionDays == null) JsonObject(current.filterKeys { it != "retentionDays" }) else current
    val retention = edited.retentionDays?.let { mapOf("retentionDays" to JsonPrimitive(it)) }.orEmpty()
    return JsonObject(
        withoutRetention + mapOf(
            "fromName" to JsonPrimitive(edited.fromName),
            "signature" to mergedSignature,
            "aliases" to JsonArray(edited.aliases.map(::JsonPrimitive)),
        ) + retention,
    )
}

private operator fun JsonObject.plus(entries: Map<String, kotlinx.serialization.json.JsonElement>): JsonObject =
    JsonObject(toMap() + entries)

private fun String.toSignatureHtml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
