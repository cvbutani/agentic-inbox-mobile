package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** One calendar day of a list: the [date], the [name] a day header shows, and its [items]. */
data class DayGroup<T>(
    val date: LocalDate,
    val name: String,
    val items: List<T>,
)

/**
 * Splits a list into calendar days for the inbox and the thread, so both read the same way.
 * Pure: the caller passes `now` and the zone, which keeps it testable and keeps day arithmetic out
 * of composables.
 */
object EmailDayGrouper {
    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    /**
     * Groups [items] by the local day of [instantOf]. Items keep their relative order; groups
     * appear in the order their day is first met. A day that reappears later in an unsorted list
     * joins its earlier group, so a header date is never repeated (it is also the lazy-list key).
     */
    fun <T> group(
        items: List<T>,
        instantOf: (T) -> Instant,
        now: Instant,
        zone: TimeZone,
    ): List<DayGroup<T>> {
        if (items.isEmpty()) return emptyList()
        val today = now.toLocalDateTime(zone).date
        val byDate = LinkedHashMap<LocalDate, MutableList<T>>()
        for (item in items) {
            byDate.getOrPut(instantOf(item).toLocalDateTime(zone).date) { mutableListOf() }.add(item)
        }
        return byDate.map { (date, dayItems) -> DayGroup(date, dayName(date, today), dayItems) }
    }

    /**
     * Parses a server timestamp for grouping. A message whose date cannot be read lands on the
     * epoch day rather than crashing the list.
     */
    fun instantOrEpoch(dateString: String): Instant =
        try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            Instant.fromEpochMilliseconds(0)
        }

    /** "Today", "Yesterday", the weekday inside a week, then "3 July" (+ year once it differs). */
    fun dayName(date: LocalDate, today: LocalDate): String = when (date.daysUntil(today)) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> EmailTimeFormatter.WEEKDAY_NAMES[date.dayOfWeek.ordinal]
        else -> {
            val dayMonth = "${date.day} ${MONTH_NAMES[date.month.ordinal]}"
            if (date.year == today.year) dayMonth else "$dayMonth ${date.year}"
        }
    }
}
