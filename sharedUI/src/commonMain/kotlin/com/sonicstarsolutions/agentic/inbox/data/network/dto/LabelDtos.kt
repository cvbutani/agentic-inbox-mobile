package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A label as the server sends it — inside a label list (with [count]) or attached to an email (without). */
@Serializable
data class LabelDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("color") val color: String = "slate",
    @SerialName("count") val count: Int = 0,
)

@Serializable
data class LabelsResponseDto(
    @SerialName("labels") val labels: List<LabelDto> = emptyList(),
)

@Serializable
data class LabelInputDto(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String,
)

/** Null fields are omitted from the body (encodeDefaults = false), which the server reads as "leave as is". */
@Serializable
data class LabelUpdateDto(
    @SerialName("name") val name: String? = null,
    @SerialName("color") val color: String? = null,
)
