package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigDto(
    @SerialName("domains") val domains: List<String> = emptyList(),
)

@Serializable
data class MailboxDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("address") val address: String,
    @SerialName("displayName") val displayName: String? = null,
)

@Serializable
data class CreateMailboxDto(
    @SerialName("address") val address: String,
    @SerialName("displayName") val displayName: String? = null,
)
