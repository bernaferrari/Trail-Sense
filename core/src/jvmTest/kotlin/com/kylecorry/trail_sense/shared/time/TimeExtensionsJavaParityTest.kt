package com.kylecorry.trail_sense.shared.time

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

/**
 * Checks the zone-aware helpers against the java.time code they replaced, in every zone, around every
 * offset transition from 1990 to 2040 and at random instants.
 */
class TimeExtensionsJavaParityTest {

    @Test
    fun matchesJavaTimeInEveryZone() {
        val random = Random(42)
        val start = java.time.Instant.parse("1990-01-01T00:00:00Z")
        val end = java.time.Instant.parse("2040-01-01T00:00:00Z")
        for (id in ZoneId.getAvailableZoneIds().sorted()) {
            val zoneId = ZoneId.of(id)
            val zone = TimeZone.of(id)
            val samples = mutableListOf<java.time.Instant>()
            var transition = zoneId.rules.nextTransition(start)
            while (transition != null && transition.instant < end) {
                for (offsetSeconds in listOf(-5400L, -2700L, -61L, -1L, 0L, 1L, 1799L, 2700L, 3599L, 5400L)) {
                    samples.add(transition.instant.plusSeconds(offsetSeconds).plusNanos(random.nextLong(1_000_000_000)))
                }
                transition = zoneId.rules.nextTransition(transition.instant)
            }
            repeat(50) {
                samples.add(java.time.Instant.ofEpochSecond(random.nextLong(start.epochSecond, end.epochSecond), random.nextLong(1_000_000_000)))
            }
            for (sample in samples) {
                val zdt = ZonedDateTime.ofInstant(sample, zoneId)
                val kt = sample.toKotlinInstant()
                check(id, sample, "truncate 1h", zdt.truncatedTo(ChronoUnit.HOURS), kt.truncateTo(1.hours, zone))
                check(id, sample, "truncate 1m", zdt.truncatedTo(ChronoUnit.MINUTES), kt.truncateTo(1.minutes, zone))
                check(id, sample, "truncate 1d", zdt.truncatedTo(ChronoUnit.DAYS), kt.truncateTo(24.hours, zone))
                check(id, sample, "round 15m", javaRoundNearestMinute(zdt, 15), kt.roundToNearest(15.minutes, zone))
                check(id, sample, "round 1m", javaRoundNearestMinute(zdt, 1), kt.roundToNearest(1.minutes, zone))
                check(id, sample, "round 7m", javaRoundNearestMinute(zdt, 7), kt.roundToNearest(7.minutes, zone))
                check(id, sample, "plusDays 1", zdt.plusDays(1), kt.plusDays(1, zone))
                check(id, sample, "plusDays -1", zdt.plusDays(-1), kt.plusDays(-1, zone))
                check(id, sample, "with 01:30", zdt.with(java.time.LocalTime.of(1, 30)), kt.withTime(LocalTime(1, 30), zone))
                check(id, sample, "with 02:30", zdt.with(java.time.LocalTime.of(2, 30)), kt.withTime(LocalTime(2, 30), zone))
                check(id, sample, "with 23:30", zdt.with(java.time.LocalTime.of(23, 30)), kt.withTime(LocalTime(23, 30), zone))
                val date = zdt.toLocalDate()
                val kDate = LocalDate(date.year, date.monthValue, date.dayOfMonth)
                assertEquals(date.atStartOfDay(zoneId).toInstant(), kDate.atStartOfDay(zone).toJavaInstant(), "LocalDate.atStartOfDay of $sample in $id")
                assertEquals(ZonedDateTime.of(date, java.time.LocalTime.MIN, zoneId).toInstant(), kt.atStartOfDay(zone).toJavaInstant(), "ZonedDateTime.atStartOfDay of $sample in $id")
                assertEquals(ZonedDateTime.of(date, java.time.LocalTime.MAX, zoneId).toInstant(), kt.atEndOfDay(zone).toJavaInstant(), "atEndOfDay of $sample in $id")
            }
        }
    }

    @Test
    fun durationConversionsMatchJava() {
        val random = Random(7)
        val samples = listOf(0L, 1L, -1L, 999_999L, -999_999L, 1_000_000L, -1_000_000L, -500_000_000L, -1_500_000_000L,
            -59_500_000_000L, -3_599_500_000_000L, -86_399_500_000_000L) +
            List(10_000) { random.nextLong(-200_000_000_000_000L, 200_000_000_000_000L) }
        for (nanos in samples) {
            val java = java.time.Duration.ofNanos(nanos)
            val kt = nanos.nanoseconds
            assertEquals(java.seconds, kt.toJavaSeconds(), "seconds of $nanos")
            // java.time truncates toMillis toward zero (JDK 9+), the same as inWholeMilliseconds
            assertEquals(java.toMillis(), kt.inWholeMilliseconds, "millis of $nanos")
            assertEquals(java.toMinutes(), kt.toJavaMinutes(), "minutes of $nanos")
            assertEquals(java.toHours(), kt.toJavaHours(), "hours of $nanos")
            assertEquals(java.toDays(), kt.toJavaDays(), "days of $nanos")
        }
    }

    private fun check(zone: String, sample: java.time.Instant, what: String, expected: ZonedDateTime, actual: kotlin.time.Instant) {
        assertEquals(expected.toInstant(), actual.toJavaInstant(), "$what of $sample in $zone")
    }

    /** The removed JavaTime.roundNearestMinute, verbatim. */
    private fun javaRoundNearestMinute(time: ZonedDateTime, minutes: Int): ZonedDateTime {
        val seconds = time.second
        if (minutes == 1) {
            return if (seconds >= 30) {
                time.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            } else {
                time.truncatedTo(ChronoUnit.MINUTES)
            }
        }
        val nanosPerMinute = 60_000_000_000L
        val intervalNanos = minutes * nanosPerMinute
        val totalNanos = time.minute * nanosPerMinute + time.second * 1_000_000_000L + time.nano
        val roundedNanos = ((totalNanos + intervalNanos / 2) / intervalNanos) * intervalNanos
        return time.plusNanos(roundedNanos - totalNanos).truncatedTo(ChronoUnit.MINUTES)
    }
}
