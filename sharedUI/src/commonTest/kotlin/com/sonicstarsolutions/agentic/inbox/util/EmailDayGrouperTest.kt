package com.sonicstarsolutions.agentic.inbox.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class EmailDayGrouperTest {

    private val utc = TimeZone.UTC

    // 2026-07-17 is a Friday.
    private val now = Instant.parse("2026-07-17T10:00:00Z")

    private fun group(vararg isoDates: String, at: Instant = now, zone: TimeZone = utc) =
        EmailDayGrouper.group(isoDates.toList(), { Instant.parse(it) }, at, zone)

    @Test
    fun `an empty list gives no groups`() {
        assertTrue(group().isEmpty())
    }

    @Test
    fun `items on the same local day share a group`() {
        val groups = group("2026-07-17T09:00:00Z", "2026-07-17T01:00:00Z")

        assertEquals(1, groups.size)
        assertEquals(LocalDate(2026, 7, 17), groups[0].date)
        assertEquals(listOf("2026-07-17T09:00:00Z", "2026-07-17T01:00:00Z"), groups[0].items)
    }

    @Test
    fun `order inside and across groups is preserved`() {
        val groups = group(
            "2026-07-17T09:00:00Z",
            "2026-07-17T08:00:00Z",
            "2026-07-16T20:00:00Z",
            "2026-07-16T07:00:00Z",
            "2026-07-10T07:00:00Z",
        )

        assertEquals(
            listOf(LocalDate(2026, 7, 17), LocalDate(2026, 7, 16), LocalDate(2026, 7, 10)),
            groups.map { it.date },
        )
        assertEquals(
            listOf(
                listOf("2026-07-17T09:00:00Z", "2026-07-17T08:00:00Z"),
                listOf("2026-07-16T20:00:00Z", "2026-07-16T07:00:00Z"),
                listOf("2026-07-10T07:00:00Z"),
            ),
            groups.map { it.items },
        )
    }

    @Test
    fun `a day that reappears out of order still yields one group`() {
        val groups = group("2026-07-17T09:00:00Z", "2026-07-16T09:00:00Z", "2026-07-17T08:00:00Z")

        assertEquals(listOf(LocalDate(2026, 7, 17), LocalDate(2026, 7, 16)), groups.map { it.date })
        assertEquals(
            listOf("2026-07-17T09:00:00Z", "2026-07-17T08:00:00Z"),
            groups[0].items,
        )
    }

    @Test
    fun `23_59 and 00_01 fall in different groups`() {
        val groups = group("2026-07-17T00:01:00Z", "2026-07-16T23:59:00Z")

        assertEquals(2, groups.size)
    }

    @Test
    fun `the device time zone decides the day`() {
        // 23:30 UTC on the 16th is already the 17th in Tokyo (UTC+9), so it joins the 17th's group.
        val tokyo = TimeZone.of("Asia/Tokyo")
        val groups = group("2026-07-17T01:00:00Z", "2026-07-16T23:30:00Z", zone = tokyo)

        assertEquals(1, groups.size)
        assertEquals(LocalDate(2026, 7, 17), groups[0].date)
    }

    @Test
    fun `today and yesterday are named`() {
        val groups = group("2026-07-17T09:00:00Z", "2026-07-16T09:00:00Z")

        assertEquals(listOf("Today", "Yesterday"), groups.map { it.name })
    }

    @Test
    fun `two to six days ago is named by weekday`() {
        val groups = group("2026-07-15T09:00:00Z", "2026-07-11T09:00:00Z")

        assertEquals(listOf("Wednesday", "Saturday"), groups.map { it.name })
    }

    @Test
    fun `a week or more ago is a day and month`() {
        val groups = group("2026-07-10T09:00:00Z", "2026-07-03T09:00:00Z", "2026-01-01T09:00:00Z")

        assertEquals(listOf("10 July", "3 July", "1 January"), groups.map { it.name })
    }

    @Test
    fun `the year appears once it differs from the current one`() {
        val groups = group("2026-01-01T09:00:00Z", "2025-12-31T09:00:00Z")

        assertEquals(listOf("1 January", "31 December 2025"), groups.map { it.name })
    }

    @Test
    fun `a message dated in the future is named by date rather than failing`() {
        val groups = group("2026-07-20T09:00:00Z")

        assertEquals(listOf("20 July"), groups.map { it.name })
    }
}
