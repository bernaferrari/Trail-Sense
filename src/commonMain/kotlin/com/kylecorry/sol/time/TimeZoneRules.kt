package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The offset of [zone] at [time] without any daylight savings.
 */
internal expect fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int

/**
 * The first instant after [time] when the offset of [zone] changes, or null if there is none.
 */
internal expect fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant?

private const val STANDARD_OFFSET_SAMPLES = 6
private val STANDARD_OFFSET_SAMPLE_STEP = 30.days
private val TRANSITION_SEARCH_STEP = 1.days
private val TRANSITION_SEARCH_HORIZON = 400.days

/**
 * Estimates the standard offset of [zone] from its offsets alone, for platforms without daylight savings rules.
 *
 * Daylight savings moves clocks forward for part of the year, so the standard offset is the smallest offset in
 * effect within half a year of [time], sampled monthly.
 */
internal fun estimateStandardOffsetSeconds(zone: TimeZone, time: Instant): Int =
    (-STANDARD_OFFSET_SAMPLES..STANDARD_OFFSET_SAMPLES).minOf {
        zone.offsetAt(time + STANDARD_OFFSET_SAMPLE_STEP * it).totalSeconds
    }

/**
 * Finds the next offset change of [zone] after [time] from its offsets alone, for platforms without transition
 * data. It searches [horizon] ahead, which covers every transition within the next year.
 */
internal fun searchNextTimeZoneTransition(
    zone: TimeZone,
    time: Instant,
    horizon: Duration = TRANSITION_SEARCH_HORIZON
): Instant? {
    val offset = zone.offsetAt(time)
    var before = time
    while (before - time < horizon) {
        val after = before + TRANSITION_SEARCH_STEP
        if (zone.offsetAt(after) != offset) {
            return bisectTransition(zone, before, after)
        }
        before = after
    }
    return null
}

/**
 * Narrows a single offset change of [zone] between [start] and [end] down to the first second of the new offset.
 */
private fun bisectTransition(zone: TimeZone, start: Instant, end: Instant): Instant {
    val offset = zone.offsetAt(start)
    var low = start
    var high = end
    while (high - low > 1.seconds) {
        val middle = Instant.fromEpochSeconds(low.epochSeconds + (high.epochSeconds - low.epochSeconds) / 2)
        if (zone.offsetAt(middle) == offset) low = middle else high = middle
    }
    return high
}
