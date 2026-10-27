package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgentKeyDto(
    @SerialName("id") val id: String,
    @SerialName("prefix") val prefix: String = "",
    @SerialName("name") val name: String,
    // Plain strings, not an enum: a newer Worker may add scopes and the list must still parse.
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("dailySendLimit") val dailySendLimit: Int? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("revokedAt") val revokedAt: String? = null,
)

@Serializable
data class CreateAgentKeyDto(
    @SerialName("name") val name: String,
    @SerialName("scopes") val scopes: List<String>,
    // Null is omitted from the body (encodeDefaults = false); the Worker rejects an explicit null.
    @SerialName("dailySendLimit") val dailySendLimit: Int? = null,
)

@Serializable
data class CreatedAgentKeyDto(
    @SerialName("key") val key: AgentKeyDto,
    @SerialName("secret") val secret: String,
)
