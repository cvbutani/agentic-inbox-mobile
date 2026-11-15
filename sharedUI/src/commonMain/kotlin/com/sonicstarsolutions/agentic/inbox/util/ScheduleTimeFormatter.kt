package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * When a scheduled message goes out. Unlike [EmailTimeFormatter], whose labels count back from
 * now, this one looks forward: "Today, 6:00 PM", "Tomorrow, 8:00 AM", then weekday and date.
 */
object ScheduleTimeFormatter {
    private val WEEKDAYS = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun format(
        sendAt: String,
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): String = try {
        val local = Instant.parse(sendAt).toLocalDateTime(zone)
        val today = now.toLocalDateTime(zone).date
        val clock = clockTime(local.hour, local.minute)
        when (today.daysUntil(local.date)) {
            0 -> "Today, $clock"
            1 -> "Tomorrow, $clock"
            else -> {
                val date = "${WEEKDAYS[local.dayOfWeek.ordinal]}, ${MONTHS[local.month.ordinal]} ${local.day}"
                if (local.year == today.year) "$date, $clock" else "$date, ${local.year}, $clock"
            }
        }
    } catch (e: Exception) {
        sendAt.take(10).ifEmpty { sendAt }
    }

    private fun clockTime(hour: Int, minute: Int): String {
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        return "$hour12:${minute.toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
    }
}
