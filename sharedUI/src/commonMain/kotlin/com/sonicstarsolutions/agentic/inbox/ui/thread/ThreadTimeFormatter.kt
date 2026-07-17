package com.sonicstarsolutions.agentic.inbox.ui.thread

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Collapsed-row time label for the thread header. Precision scales down as a message ages, the
 * way most mail clients do it: a clock time inside the last 24 hours, "Yesterday" the calendar
 * day after that, the weekday name for the rest of the week, then a plain date beyond a week.
 */
object ThreadTimeFormatter {
    private val WEEKDAY_NAMES = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val MONTH_ABBREVIATIONS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun formatCollapsedTime(
        dateString: String,
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): String = try {
        val messageInstant = Instant.parse(dateString)
        if (now - messageInstant < 24.hours) {
            formatClockTime(messageInstant, zone)
        } else {
            val today = now.toLocalDateTime(zone).date
            val messageDate = messageInstant.toLocalDateTime(zone).date
            when (val daysAgo = messageDate.daysUntil(today)) {
                1 -> "Yesterday"
                in 2..6 -> WEEKDAY_NAMES[messageDate.dayOfWeek.ordinal]
                else -> {
                    val monthDay = "${MONTH_ABBREVIATIONS[messageDate.month.ordinal]} ${messageDate.day}"
                    if (messageDate.year == today.year) monthDay else "$monthDay, ${messageDate.year}"
                }
            }
        }
    } catch (e: Exception) {
        dateString.take(10)
    }

    private fun formatClockTime(instant: Instant, zone: TimeZone): String {
        val local = instant.toLocalDateTime(zone)
        val hour12 = when (val hour = local.hour % 12) { 0 -> 12; else -> hour }
        val amPm = if (local.hour < 12) "AM" else "PM"
        val minute = local.minute.toString().padStart(2, '0')
        return "$hour12:$minute $amPm"
    }
}
