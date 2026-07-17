package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EmailTimeFormatterTest {

    private val utc = TimeZone.UTC

    @Test
    fun `a message from a few hours ago shows a clock time`() {
        val now = Instant.parse("2026-07-17T10:00:00Z")
        val message = "2026-07-17T08:15:00Z" // 1h45m ago

        assertEquals("8:15 AM", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from just under 24 hours ago still shows a clock time`() {
        val now = Instant.parse("2026-07-17T10:00:00Z")
        val message = "2026-07-16T10:01:00Z" // 23h59m ago

        assertEquals("10:01 AM", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from exactly 24 hours ago no longer shows a clock time`() {
        val now = Instant.parse("2026-07-17T10:00:00Z")
        val message = "2026-07-16T10:00:00Z" // exactly 24h ago -> yesterday, same calendar day offset

        assertEquals("Yesterday", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from yesterday shows Yesterday`() {
        val now = Instant.parse("2026-07-17T10:00:00Z")
        val message = "2026-07-16T09:00:00Z" // 25h ago, calendar-day before

        assertEquals("Yesterday", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from 2 days ago shows the weekday name`() {
        // 2024-01-01 is a known Monday; 2024-01-08 is the following Monday.
        val now = Instant.parse("2024-01-08T10:00:00Z")
        val message = "2024-01-06T09:00:00Z" // Saturday, 2 days ago

        assertEquals("Saturday", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from 6 days ago still shows the weekday name`() {
        val now = Instant.parse("2024-01-08T10:00:00Z")
        val message = "2024-01-02T09:00:00Z" // Tuesday, 6 days ago

        assertEquals("Tuesday", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from exactly 7 days ago shows a date instead of a weekday`() {
        val now = Instant.parse("2024-01-08T10:00:00Z")
        val message = "2024-01-01T09:00:00Z" // Monday, 7 days ago

        assertEquals("Jan 1", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from earlier the same year shows month and day without a year`() {
        val now = Instant.parse("2024-03-01T10:00:00Z")
        val message = "2024-02-20T09:00:00Z"

        assertEquals("Feb 20", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `a message from a previous year includes the year`() {
        val now = Instant.parse("2026-01-05T10:00:00Z")
        val message = "2025-12-20T09:00:00Z"

        assertEquals("Dec 20, 2025", EmailTimeFormatter.format(message, now, utc))
    }

    @Test
    fun `an unparseable date string falls back to its first 10 characters`() {
        assertEquals("not-a-date", EmailTimeFormatter.format("not-a-date-string", Instant.parse("2026-01-05T10:00:00Z"), utc))
    }
}
