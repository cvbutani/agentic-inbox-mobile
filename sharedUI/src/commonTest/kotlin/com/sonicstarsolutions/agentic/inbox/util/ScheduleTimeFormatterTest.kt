package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ScheduleTimeFormatterTest {

    private val zone = TimeZone.UTC
    private val now = Instant.parse("2026-11-18T09:30:00Z") // a Wednesday

    @Test
    fun `later the same day reads Today`() {
        assertEquals("Today, 6:00 PM", ScheduleTimeFormatter.format("2026-11-18T18:00:00Z", now, zone))
    }

    @Test
    fun `the next day reads Tomorrow`() {
        assertEquals("Tomorrow, 8:00 AM", ScheduleTimeFormatter.format("2026-11-19T08:00:00.000Z", now, zone))
    }

    @Test
    fun `later in the same year shows weekday, month and day`() {
        assertEquals("Mon, Nov 23, 8:00 AM", ScheduleTimeFormatter.format("2026-11-23T08:00:00Z", now, zone))
    }

    @Test
    fun `another year adds the year`() {
        assertEquals("Fri, Jan 1, 2027, 12:05 AM", ScheduleTimeFormatter.format("2027-01-01T00:05:00Z", now, zone))
    }

    @Test
    fun `a time that already passed still reads as a date`() {
        assertEquals("Tue, Nov 17, 3:00 PM", ScheduleTimeFormatter.format("2026-11-17T15:00:00Z", now, zone))
    }

    @Test
    fun `an unparseable value falls back to its date part`() {
        assertEquals("not-a-date", ScheduleTimeFormatter.format("not-a-date", now, zone))
    }
}
