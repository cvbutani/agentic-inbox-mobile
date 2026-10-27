package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FeatureFlagsDto(
    /** Whether the AI email agent and auto-draft are active on this Worker instance. */
    @SerialName("aiAgent") val aiAgent: Boolean = true,
    /** Whether the MCP server endpoint is exposed. */
    @SerialName("mcp") val mcp: Boolean = false,
    /** Inbound spam classification runs on the Worker. */
    @SerialName("spamClassification") val spamClassification: Boolean = false,
    /** `POST …/ai/compose-rewrite` is served. Off by default so a legacy Worker hides the button. */
    @SerialName("aiComposeRewrite") val aiComposeRewrite: Boolean = false,
    @SerialName("signatures") val signatures: Boolean = true,
    @SerialName("autoReply") val autoReply: Boolean = false,
    @SerialName("agentAccess") val agentAccess: Boolean = false,
) {
    /** The rewrite button needs the endpoint *and* the AI agent switched on — with
     * `ENABLE_AI_AGENT=false` all AI affordances go away together. */
    val aiRewriteAvailable: Boolean get() = aiAgent && aiComposeRewrite
}

@Serializable
data class ConfigDto(
    @SerialName("domains") val domains: List<String> = emptyList(),
    @SerialName("emailAddresses") val emailAddresses: List<String> = emptyList(),
    /** Feature flags from the Worker. Absent on legacy Workers — defaults are safe fallbacks. */
    @SerialName("features") val features: FeatureFlagsDto = FeatureFlagsDto(),
)

@Serializable
data class MailboxDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    // Free-form per-mailbox preferences (forwarding/signature/autoReply); only present on the
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
