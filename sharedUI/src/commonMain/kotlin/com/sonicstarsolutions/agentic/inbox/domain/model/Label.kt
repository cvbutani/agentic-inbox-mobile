package com.sonicstarsolutions.agentic.inbox.domain.model

/** A coloured tag a message can carry (any number per message). [count] is how many messages
 * wear it; it is only filled in by the label list, not by the labels attached to an email. */
data class Label(
    val id: String,
    val name: String,
    val color: String = LabelColors.DEFAULT,
    val count: Int = 0,
)

/** The colour names the server accepts for a label, in display order. */
object LabelColors {
    const val DEFAULT = "slate"
    val all: List<String> = listOf("slate", "red", "orange", "amber", "green", "teal", "blue", "purple")
}
