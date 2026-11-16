package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ConfigDto(
    @SerialName("domains") val domains: List<String> = emptyList(),
    @SerialName("emailAddresses") val emailAddresses: List<String> = emptyList(),
)

@Serializable
data class MailboxDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    // The owner's display name. The list endpoint sends it (null when unset); the single-mailbox
    // GET carries it inside `settings` instead.
    @SerialName("fromName") val fromName: String? = null,
    // Free-form per-mailbox preferences (signature, aliases, auto-draft, …); only present on the
    // single-mailbox GET, omitted by the list endpoint. Untyped here since the app doesn't edit it.
    @SerialName("settings") val settings: JsonElement? = null,
)

@Serializable
data class CreateMailboxDto(
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("settings") val settings: JsonElement? = null,
)

@Serializable
data class UpdateMailboxDto(
    @SerialName("settings") val settings: JsonElement,
)

@Serializable
data class FolderDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("unreadCount") val unreadCount: Int = 0,
)

@Serializable
data class FolderNameDto(
    @SerialName("name") val name: String,
)
