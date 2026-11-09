package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeZoneRulesTest {

    private val newYork = TimeZone.of("America/New_York")
    private val sydney = TimeZone.of("Australia/Sydney")
    private val shanghai = TimeZone.of("Asia/Shanghai")

    // The platform rules and the offset-only estimate used on the web must agree
    private val standardOffsetRules = listOf(::standardOffsetSeconds, ::estimateStandardOffsetSeconds)
    private val transitionRules = listOf(
        ::nextTimeZoneTransition,
        { zone: TimeZone, time: Instant -> searchNextTimeZoneTransition(zone, time) }
    )

    @Test
    fun standardOffsetIgnoresDaylightSavings() {
        for (rule in standardOffsetRules) {
            assertEquals(-5 * 3600, rule(newYork, Instant.parse("2022-07-01T00:00:00Z")))
            assertEquals(-5 * 3600, rule(newYork, Instant.parse("2022-01-01T00:00:00Z")))
            assertEquals(-5 * 3600, rule(newYork, Instant.parse("2022-03-13T07:00:00Z")))
            assertEquals(-5 * 3600, rule(newYork, Instant.parse("2022-11-06T05:59:59Z")))
            assertEquals(10 * 3600, rule(sydney, Instant.parse("2022-01-01T00:00:00Z")))
            assertEquals(8 * 3600, rule(shanghai, Instant.parse("2022-07-01T00:00:00Z")))
            assertEquals(0, rule(TimeZone.UTC, Instant.parse("2022-07-01T00:00:00Z")))
        }
    }

    @Test
    fun nextTransitionFindsTheExactInstant() {
        for (rule in transitionRules) {
            assertEquals(
                Instant.parse("2022-03-13T07:00:00Z"),
                rule(newYork, Instant.parse("2022-01-01T00:00:00Z"))
            )
            assertEquals(
                Instant.parse("2022-11-06T06:00:00Z"),
                rule(newYork, Instant.parse("2022-03-13T07:00:00Z"))
            )
            assertEquals(
                Instant.parse("2022-04-02T16:00:00Z"),
                rule(sydney, Instant.parse("2022-01-01T00:00:00Z"))
            )
        }
    }

    @Test
    fun nextTransitionIsNullWithoutDaylightSavings() {
        for (rule in transitionRules) {
            assertEquals(null, rule(shanghai, Instant.parse("2022-01-01T00:00:00Z")))
            assertEquals(null, rule(TimeZone.UTC, Instant.parse("2022-01-01T00:00:00Z")))
        }
    }
}
