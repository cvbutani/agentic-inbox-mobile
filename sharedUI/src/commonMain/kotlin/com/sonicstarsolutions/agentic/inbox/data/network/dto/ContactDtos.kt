package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    @SerialName("address") val address: String,
    @SerialName("count") val count: Int = 0,
)

@Serializable
data class ContactsResponseDto(
    @SerialName("contacts") val contacts: List<ContactDto> = emptyList(),
)
