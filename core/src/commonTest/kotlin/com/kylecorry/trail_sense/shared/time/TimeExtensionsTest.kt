package com.kylecorry.trail_sense.shared.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimeExtensionsTest {

    private val utc = TimeZone.UTC
    private val kathmandu = TimeZone.of("Asia/Kathmandu") // +05:45
    private val newYork = TimeZone.of("America/New_York")
    private val lordHowe = TimeZone.of("Australia/Lord_Howe") // 30 minute DST shift

    @Test
    fun truncateToHourUsesLocalHour() {
        assertEquals(t("2026-11-09T10:00:00Z"), t("2026-11-09T10:59:59.999Z").truncateTo(1.hours, utc))
        // 16:05:30 local -> 16:00 local
        assertEquals(t("2026-11-09T10:15:00Z"), t("2026-11-09T10:20:30Z").truncateTo(1.hours, kathmandu))
        assertEquals(t("2026-11-09T10:15:00Z"), t("2026-11-09T10:15:00Z").truncateTo(1.hours, kathmandu))
        assertEquals(t("2026-11-09T09:15:00Z"), t("2026-11-09T10:14:59Z").truncateTo(1.hours, kathmandu))
    }

    @Test
    fun truncateToMinutesAndDays() {
        assertEquals(t("2026-11-09T10:15:00Z"), t("2026-11-09T10:29:59Z").truncateTo(15.minutes, kathmandu))
        assertEquals(t("2026-11-09T10:20:00Z"), t("2026-11-09T10:20:59.5Z").truncateTo(1.minutes, kathmandu))
        // Local midnight in Kathmandu is 18:15 UTC the day before
        assertEquals(t("2026-11-08T18:15:00Z"), t("2026-11-09T10:20:30Z").truncateTo(24.hours, kathmandu))
    }

    @Test
    fun truncateKeepsOffsetInDstOverlap() {
        // 2026-11-01 01:00-02:00 happens twice in New York (EDT -4, then EST -5)
        assertEquals(t("2026-11-01T05:00:00Z"), t("2026-11-01T05:30:00Z").truncateTo(1.hours, newYork))
        assertEquals(t("2026-11-01T06:00:00Z"), t("2026-11-01T06:30:00Z").truncateTo(1.hours, newYork))
    }

    @Test
    fun truncateIntoDstGapShiftsForward() {
        // Spring forward in New York: 2026-03-08 02:00 -> 03:00
        assertEquals(t("2026-03-08T07:00:00Z"), t("2026-03-08T07:15:00Z").truncateTo(1.hours, newYork))
        // Lord Howe: 2026-10-04 02:00 -> 02:30 (+10:30 -> +11). 02:45 truncates into the gap at 02:00,
        // which java.time resolves to 02:30 +11:00.
        assertEquals(t("2026-10-03T15:30:00Z"), t("2026-10-03T15:45:00Z").truncateTo(1.hours, lordHowe))
    }

    @Test
    fun roundToNearestUsesLocalHour() {
        assertEquals(t("2026-11-09T10:15:00Z"), t("2026-11-09T10:20:29Z").roundToNearest(15.minutes, kathmandu))
        assertEquals(t("2026-11-09T10:30:00Z"), t("2026-11-09T10:22:30Z").roundToNearest(15.minutes, kathmandu))
        assertEquals(t("2026-11-09T10:30:00Z"), t("2026-11-09T10:22:30Z").roundToNearest(15.minutes, utc))
        assertEquals(t("2026-11-09T10:15:00Z"), t("2026-11-09T10:22:29.999Z").roundToNearest(15.minutes, utc))
        // 7-minute steps restart every local hour: 16:40 local (10:55Z) is 40 min into the hour -> 42
        assertEquals(t("2026-11-09T10:57:00Z"), t("2026-11-09T10:55:00Z").roundToNearest(7.minutes, kathmandu))
        assertEquals(t("2026-11-09T10:56:00Z"), t("2026-11-09T10:55:00Z").roundToNearest(7.minutes, utc))
    }

    @Test
    fun roundToNearestMinute() {
        assertEquals(t("2026-11-09T10:21:00Z"), t("2026-11-09T10:20:30Z").roundToNearest(1.minutes, kathmandu))
        assertEquals(t("2026-11-09T10:20:00Z"), t("2026-11-09T10:20:29.999Z").roundToNearest(1.minutes, kathmandu))
    }

    @Test
    fun roundToNearestAcrossDstOverlap() {
        // 01:52:30 EST (second pass) rounds up to 02:00 EST
        assertEquals(t("2026-11-01T07:00:00Z"), t("2026-11-01T06:52:30Z").roundToNearest(15.minutes, newYork))
        // 01:52:30 EDT (first pass) rounds up to 02:00 EDT, which is 01:00 EST
        assertEquals(t("2026-11-01T06:00:00Z"), t("2026-11-01T05:52:30Z").roundToNearest(15.minutes, newYork))
    }

    @Test
    fun decodesStoredDates() {
        assertEquals(LocalDate(2026, 11, 9), decodeStoredLocalDate("2026-11-09"))
        assertEquals(LocalDate(2026, 1, 1), decodeStoredLocalDate(LocalDate(2026, 1, 1).toString()))
        assertNull(decodeStoredLocalDate(null))
        assertNull(decodeStoredLocalDate(""))
        assertNull(decodeStoredLocalDate("-999999999-01-01")) // java.time LocalDate.MIN ("never sent")
        assertNull(decodeStoredLocalDate("not a date"))
        assertNull(decodeStoredLocalDate("2026-02-30"))
    }

    @Test
    fun plusDaysIsCalendarBased() {
        // Across the New York spring forward: same wall clock time, 23 hours later
        assertEquals(t("2026-03-08T16:00:00Z"), t("2026-03-07T17:00:00Z").plusDays(1, newYork))
        // Across fall back: 25 hours later
        assertEquals(t("2026-11-01T17:00:00Z"), t("2026-10-31T16:00:00Z").plusDays(1, newYork))
        // Landing in the overlap keeps the current (daylight) offset, like ZonedDateTime.plusDays
        assertEquals(t("2026-11-01T05:30:00Z"), t("2026-10-31T05:30:00Z").plusDays(1, newYork))
        // Landing in the gap shifts forward by the gap length: 02:30 -> 03:30 EDT
        assertEquals(t("2026-03-08T07:30:00Z"), t("2026-03-07T07:30:00Z").plusDays(1, newYork))
        assertEquals(t("2026-11-10T10:20:30Z"), t("2026-11-09T10:20:30Z").plusDays(1, kathmandu))
    }

    @Test
    fun withTimeKeepsDateAndOffset() {
        assertEquals(t("2026-11-09T12:15:00Z"), t("2026-11-09T10:20:30Z").withTime(LocalTime(18, 0), kathmandu))
        // 01:30 on the fall back day keeps the offset of the original instant
        assertEquals(t("2026-11-01T05:30:00Z"), t("2026-11-01T04:00:00Z").withTime(LocalTime(1, 30), newYork))
        assertEquals(t("2026-11-01T06:30:00Z"), t("2026-11-01T06:50:00Z").withTime(LocalTime(1, 30), newYork))
    }

    @Test
    fun endOfDayUsesLocalMaxTime() {
        // Brazil used to fall back at midnight (00:00 -> 23:00), so 23:xx on 2018-02-17 happened twice.
        // java.time picks the earlier offset (-02:00).
        val saoPaulo = TimeZone.of("America/Sao_Paulo")
        assertEquals(t("2018-02-18T01:59:59.999999999Z"), LocalDate(2018, 2, 17).atEndOfDay(saoPaulo))
        assertEquals(t("2026-11-09T23:59:59.999999999Z"), LocalDate(2026, 11, 9).atEndOfDay(utc))
    }

    @Test
    fun javaDurationConversionsFloorToTheSecond() {
        assertEquals(-1L, (-300).milliseconds.toJavaSeconds())
        assertEquals(0L, (-300).milliseconds.inWholeSeconds)
        assertEquals(-2L, (-1500).milliseconds.toJavaSeconds())
        assertEquals(-1L, (-1).seconds.toJavaSeconds())
        assertEquals(1L, 1999.milliseconds.toJavaSeconds())
        assertEquals(-1L, (-59.5).seconds.toJavaMinutes())
        assertEquals(-1L, (-90.5).seconds.toJavaMinutes())
        assertEquals(0L, (-59).seconds.toJavaMinutes())
        assertEquals(-1L, (-3599.5).seconds.toJavaHours())
        assertEquals(1L, 3600.seconds.toJavaHours())
        assertEquals(0L, (-1).nanoseconds.toJavaDays())
        assertEquals(-1L, (-86_400.5).seconds.toJavaDays())
    }

    private fun t(value: String): Instant = Instant.parse(value)
}
