package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The quick times offered for "Send later", and the limits the server puts on a scheduled send. */
object SendLater {

    data class Option(val label: String, val at: Instant)

    enum class Problem(val message: String) {
        TOO_SOON("Pick a time at least a couple of minutes from now."),
        TOO_FAR("Pick a time within the next 90 days."),
    }

    // The server rejects anything under a minute or over 90 days away; the margin on the near
    // side absorbs clock drift and the time the request takes to arrive.
    private val MIN_LEAD = 2.minutes
    private const val MAX_LEAD_DAYS = 90

    /** Later today (evening, if it is still more than two hours off), tomorrow morning and
     * afternoon, and Monday morning — each in [zone], skipping any that would repeat another. */
    fun presets(now: Instant, zone: TimeZone): List<Option> {
        val today = now.toLocalDateTime(zone).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        fun on(date: LocalDate, hour: Int): Instant = LocalDateTime(date.year, date.month, date.day, hour, 0).toInstant(zone)

        val options = mutableListOf<Option>()
        val evening = on(today, 18)
        if (evening - now > 2.hours) options += Option("Later today", evening)
        options += Option("Tomorrow morning", on(tomorrow, 8))
        options += Option("Tomorrow afternoon", on(tomorrow, 13))

        // Next Monday strictly after today; when that is tomorrow it would repeat the pick above.
        val daysToMonday = 7 - today.dayOfWeek.ordinal
        if (daysToMonday != 1) options += Option("Monday morning", on(today.plus(daysToMonday, DateTimeUnit.DAY), 8))
        return options
    }

    /** Why [at] can't be used, or null when it can. */
    fun problemWith(at: Instant, now: Instant): Problem? = when {
        at < now + MIN_LEAD -> Problem.TOO_SOON
        at > now + MAX_LEAD_DAYS.days -> Problem.TOO_FAR
        else -> null
    }

    /** Joins the date picker's result (the chosen day as midnight UTC, in epoch millis) with a
     * clock time into the instant that wall-clock moment is in [zone]. */
    fun combine(pickedDayUtcMillis: Long, hour: Int, minute: Int, zone: TimeZone): Instant {
        val day = Instant.fromEpochMilliseconds(pickedDayUtcMillis).toLocalDateTime(TimeZone.UTC).date
        return LocalDateTime(day.year, day.month, day.day, hour, minute).toInstant(zone)
    }
}
