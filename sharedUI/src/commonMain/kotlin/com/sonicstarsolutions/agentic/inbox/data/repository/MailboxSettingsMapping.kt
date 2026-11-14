package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private fun JsonObject?.string(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun JsonObject?.boolean(key: String): Boolean? = (this?.get(key) as? JsonPrimitive)?.booleanOrNull

private fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

/** Reads the editable fields out of a mailbox's free-form settings. Missing or wrongly typed
 * values become defaults — a settings blob written by another client must never crash the screen. */
internal fun JsonObject?.toMailboxSettings(): MailboxSettings {
    val signature = obj("signature")
    return MailboxSettings(
        fromName = string("fromName").orEmpty(),
        signatureEnabled = signature.boolean("enabled") ?: false,
        signatureText = signature.string("text").orEmpty(),
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

    return JsonObject(
        current + mapOf(
            "fromName" to JsonPrimitive(edited.fromName),
            "signature" to mergedSignature,
        ),
    )
}

private operator fun JsonObject.plus(entries: Map<String, kotlinx.serialization.json.JsonElement>): JsonObject =
    JsonObject(toMap() + entries)

private fun String.toSignatureHtml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
