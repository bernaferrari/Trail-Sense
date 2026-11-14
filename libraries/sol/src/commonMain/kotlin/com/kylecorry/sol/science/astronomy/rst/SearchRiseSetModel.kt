package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.AstroSearch
import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.ICelestialLocator
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal class SearchRiseSetModel {
    @Suppress("LongParameterList")
    fun calculate(
        locator: ICelestialLocator, date: LocalDate, zone: TimeZone, location: Coordinate,
        standardAltitude: Double, withRefraction: Boolean, withParallax: Boolean
    ): RiseSetTransitTimes {
        fun altitude(time: Instant): Float =
            AstroUtils.getAltitude(locator, time, location, withRefraction, withParallax)
        fun refinedAltitude(time: Instant): Float =
            altitude(Instant.fromEpochSeconds(time.toEpochMilliseconds() / 1000))
        fun searchRange(range: Range<Instant>) = Range(range.start - 1.minutes, range.end + 1.minutes)

        val ranges = findRanges(date, zone, standardAltitude, ::altitude)
        val rise = ranges.rise?.let {
            AstroSearch.findStart(searchRange(it), 1.minutes) { t -> refinedAltitude(t) >= standardAltitude }
        }
        val set = ranges.set?.let {
            AstroSearch.findEnd(searchRange(it), 1.minutes) { t -> refinedAltitude(t) >= standardAltitude }
        }
        val transit = ranges.transit?.let {
            val peak = AstroSearch.findPeak(searchRange(it), 1.minutes) { t -> refinedAltitude(t) }
            peak.takeIf { t -> altitude(t) >= standardAltitude }
        }
        return RiseSetTransitTimes(rise, transit, set)
    }

    private fun findRanges(
        date: LocalDate, zone: TimeZone, standardAltitude: Double, altitude: (Instant) -> Float
    ): EventRanges {
        var time = date.atStartOfDayIn(zone)
        var lastAltitude = altitude(time)
        var rising = lastAltitude > altitude(time - 1.hours)
        var rise: Range<Instant>? = null
        var set: Range<Instant>? = null
        var transit: Range<Instant>? = null
        while (time.toLocalDateTime(zone).date == date) {
            val currentAltitude = altitude(time)
            if (lastAltitude >= standardAltitude && currentAltitude < standardAltitude) {
                set = Range(time - 1.hours, time)
            }
            if (standardAltitude in lastAltitude.toDouble()..<currentAltitude.toDouble()) {
                rise = Range(time - 1.hours, time)
            }
            if (transit == null) {
                val currentlyRising = currentAltitude > altitude(time - 1.minutes)
                if (rising && !currentlyRising && currentAltitude >= standardAltitude) {
                    transit = Range(time - 1.hours, time)
                }
                rising = currentlyRising
            }
            if (set != null && rise != null && transit != null) break
            lastAltitude = currentAltitude
            time += 1.hours
        }
        return EventRanges(rise, transit, set)
    }

    private data class EventRanges(
        val rise: Range<Instant>?, val transit: Range<Instant>?, val set: Range<Instant>?
    )
}
