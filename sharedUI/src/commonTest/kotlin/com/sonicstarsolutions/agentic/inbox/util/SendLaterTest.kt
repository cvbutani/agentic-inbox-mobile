package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SendLaterTest {

    private val zone = TimeZone.UTC

    /** 2026-11-18 is a Wednesday. */
    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 11): Instant =
        LocalDateTime(2026, month, day, hour, minute).toInstant(zone)

    @Test
    fun `on a Wednesday morning the quick picks are later today, tomorrow and Monday`() {
        val options = SendLater.presets(now = at(18, 9), zone = zone)

        assertEquals(
            listOf(
                "Later today" to at(18, 18),
                "Tomorrow morning" to at(19, 8),
                "Tomorrow afternoon" to at(19, 13),
                "Monday morning" to at(23, 8),
            ),
            options.map { it.label to it.at },
        )
    }

    @Test
    fun `later today is dropped once it would be less than two hours away`() {
        val labels = SendLater.presets(now = at(18, 16, 30), zone = zone).map { it.label }

        assertEquals(listOf("Tomorrow morning", "Tomorrow afternoon", "Monday morning"), labels)
    }

    @Test
    fun `on Sunday tomorrow is Monday so there is no separate Monday pick`() {
        // 2026-11-22 is a Sunday.
        val labels = SendLater.presets(now = at(22, 10), zone = zone).map { it.label }

        assertEquals(listOf("Later today", "Tomorrow morning", "Tomorrow afternoon"), labels)
    }

    @Test
    fun `on a Monday the Monday pick is a week away`() {
        // 2026-11-23 is a Monday.
        val monday = SendLater.presets(now = at(23, 9), zone = zone).first { it.label == "Monday morning" }

        assertEquals(at(30, 8), monday.at)
    }

    @Test
    fun `picks follow the given time zone`() {
        val tokyo = TimeZone.of("Asia/Tokyo")
        val now = LocalDateTime(2026, 11, 18, 9, 0).toInstant(tokyo)

        val tomorrow = SendLater.presets(now = now, zone = tokyo).first { it.label == "Tomorrow morning" }

        assertEquals(LocalDateTime(2026, 11, 19, 8, 0).toInstant(tokyo), tomorrow.at)
    }

    @Test
    fun `a time a few minutes ahead or a few months ahead is accepted`() {
        val now = at(18, 9)

        assertNull(SendLater.problemWith(now + 5.minutes, now))
        assertNull(SendLater.problemWith(now + 60.days, now))
    }

    @Test
    fun `a time in the past or within two minutes is too soon`() {
        val now = at(18, 9)

        assertEquals(SendLater.Problem.TOO_SOON, SendLater.problemWith(now - 1.minutes, now))
        assertEquals(SendLater.Problem.TOO_SOON, SendLater.problemWith(now + 1.minutes, now))
    }

    @Test
    fun `more than ninety days ahead is too far`() {
        val now = at(18, 9)

        assertEquals(SendLater.Problem.TOO_FAR, SendLater.problemWith(now + 91.days, now))
    }

    @Test
    fun `the messages say what to do`() {
        assertEquals("Pick a time at least a couple of minutes from now.", SendLater.Problem.TOO_SOON.message)
        assertEquals("Pick a time within the next 90 days.", SendLater.Problem.TOO_FAR.message)
    }

    @Test
    fun `a picked calendar day and clock time combine in the given zone`() {
        // The date picker reports the chosen day as midnight UTC, whatever the device's zone.
        val pickedDay = LocalDateTime(2026, 11, 19, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val tokyo = TimeZone.of("Asia/Tokyo")

        val result = SendLater.combine(pickedDay, hour = 8, minute = 30, zone = tokyo)

        assertEquals(LocalDateTime(2026, 11, 19, 8, 30).toInstant(tokyo), result)
    }
}
