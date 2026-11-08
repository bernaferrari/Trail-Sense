@file:Suppress("MaxLineLength", "ReturnCount", "LoopWithTooManyJumpStatements")

package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.astronomy.locators.MeteorShowerPositionModel
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.rst.NewtonsRiseSetModel
import com.kylecorry.sol.science.astronomy.rst.SearchRiseSetTimes
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal data class MeteorPeak(val shower: MeteorShower, val start: Instant, val peak: Instant, val end: Instant)

internal object MeteorModel {
    fun shower(location: Coordinate, date: Instant, zone: TimeZone): MeteorPeak? {
        val start = date.toLocalDateTime(zone).date.atStartOfDayIn(zone)
        val longitude = SunCalculationModel.longitude(date)
        for (shower in MeteorShower.entries) {
            if (deltaAngle(longitude, shower.solarLongitude).absoluteValue > 2) continue
            val peak = peak(shower, location, start, zone) ?: continue
            val transit = peak.transit ?: continue
            if (transit.toLocalDateTime(zone).date == date.toLocalDateTime(zone).date) {
                return MeteorPeak(shower, peak.rise ?: transit, transit, peak.set ?: transit)
            }
        }
        return null
    }

    fun active(location: Coordinate, date: Instant, zone: TimeZone): List<MeteorPeak> {
        val active = mutableSetOf<MeteorPeak>()
        val range = MeteorShower.entries.maxOf { it.activeDays }
        var current = date.plus(-range, DateTimeUnit.DAY, zone)
        val end = date.plus(range, DateTimeUnit.DAY, zone)
        while (current < end) {
            val peak = shower(location, current, zone)
            if (peak != null && (date - peak.peak).absoluteValue <= (peak.shower.activeDays / 2).days) active.add(peak)
            current = current.plus(1, DateTimeUnit.DAY, zone)
        }
        return active.toList()
    }

    fun position(shower: MeteorShower, location: Coordinate, time: Instant): CelestialObservation {
        val horizon = AstroUtils.getLocation(MeteorShowerPositionModel(shower), time, location)
        return CelestialObservation(Bearing.from(horizon.azimuth.toFloat()), horizon.altitude.toFloat())
    }

    private fun peak(shower: MeteorShower, location: Coordinate, now: Instant, zone: TimeZone): SearchRiseSetTimes? {
        val time = SunCalculationModel.nextAtLongitude(shower.solarLongitude, now, zone)
        fun times(date: Instant) = NewtonsRiseSetModel().calculate(
            MeteorShowerPositionModel(shower), date, zone, location, 0.0, false, false
        )
        val results = listOf(times(time.plus(-1, DateTimeUnit.DAY, zone)), times(time),
            times(time.plus(1, DateTimeUnit.DAY, zone)))
        val transit = results.mapNotNull { it.transit }.minByOrNull { (it - time).absoluteValue }
        val reference = transit ?: time
        val rise = results.mapNotNull { it.rise }.filter { it < reference }.maxOrNull()
        val set = results.mapNotNull { it.set }.filter { it > reference }.minOrNull()
        val night = SunCalculationModel.closestNight(reference, zone, location, SunTimesMode.Astronomical) ?: return null
        if (transit == null) {
            var current = night.start
            var peakAltitude = -1f
            var peakTime = current
            while (current < night.end) {
                val altitude = position(shower, location, current).altitude
                if (altitude > peakAltitude) {
                    peakAltitude = altitude
                    peakTime = current
                }
                current += 5.minutes
            }
            return if (peakAltitude < 0) null else SearchRiseSetTimes(night.start, peakTime, night.end)
        }
        val intersection = Range(rise ?: night.start, set ?: night.end).intersection(night) ?: return null
        return SearchRiseSetTimes(intersection.start, intersection.clamp(transit), intersection.end)
    }
}
