package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.astronomy.locators.MeteorShowerPositionModel
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.science.astronomy.rst.NewtonsRiseSetModel
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

internal object MeteorModel {
    /**
     * Gets the meteor shower that peaks on the local day containing [date], if any.
     */
    fun shower(location: Coordinate, date: Instant, zone: TimeZone): MeteorShowerPeak? {
        val day = date.toLocalDateTime(zone).date
        val startOfDay = day.atStartOfDayIn(zone)
        val solarLongitude = SunCalculationModel.longitude(date)
        return MeteorShower.entries
            .asSequence()
            .filter { deltaAngle(solarLongitude, it.solarLongitude).absoluteValue <= 2 }
            .mapNotNull { shower ->
                val peak = peak(shower, location, startOfDay, zone) ?: return@mapNotNull null
                val transit = peak.transit ?: return@mapNotNull null
                MeteorShowerPeak(shower, peak.rise ?: transit, transit, peak.set ?: transit)
            }
            .firstOrNull { it.peak.toLocalDateTime(zone).date == day }
    }

    fun active(location: Coordinate, date: Instant, zone: TimeZone): List<MeteorShowerPeak> {
        val active = mutableSetOf<MeteorShowerPeak>()
        val range = MeteorShower.entries.maxOf { it.activeDays }
        var current = date.plus(-range, DateTimeUnit.DAY, zone)
        val end = date.plus(range, DateTimeUnit.DAY, zone)
        while (current < end) {
            val peak = shower(location, current, zone)
            if (peak != null && (date - peak.peak).absoluteValue <= (peak.shower.activeDays / 2).days) {
                active.add(peak)
            }
            current = current.plus(1, DateTimeUnit.DAY, zone)
        }
        return active.toList()
    }

    fun position(shower: MeteorShower, location: Coordinate, time: Instant): CelestialObservation {
        val horizon = AstroUtils.getLocation(MeteorShowerPositionModel(shower), time, location)
        return CelestialObservation(Bearing.from(horizon.azimuth.toFloat()), horizon.altitude.toFloat())
    }

    /**
     * Gets the visible window (rise to set, clipped to the astronomical night) and peak of the radiant.
     */
    @Suppress("ReturnCount")
    private fun peak(shower: MeteorShower, location: Coordinate, now: Instant, zone: TimeZone): RiseSetTransitTimes? {
        val time = SunCalculationModel.nextAtLongitude(shower.solarLongitude, now, zone)
        val radiant = MeteorShowerPositionModel(shower)
        val results = (-1..1).map {
            val day = time.plus(it, DateTimeUnit.DAY, zone)
            NewtonsRiseSetModel().calculate(radiant, day, zone, location, 0.0, false, false)
        }
        val transit = results.mapNotNull { it.transit }.minByOrNull { (it - time).absoluteValue }
        val reference = transit ?: time
        val rise = results.mapNotNull { it.rise }.filter { it < reference }.maxOrNull()
        val set = results.mapNotNull { it.set }.filter { it > reference }.minOrNull()
        val night = SunCalculationModel.closestNight(reference, zone, location, SunTimesMode.Astronomical)
            ?: return null

        if (transit == null) {
            // The radiant never transits, use its highest point during the night instead
            val peakTime = generateSequence(night.start) { it + PEAK_SEARCH_STEP }
                .takeWhile { it < night.end }
                .maxByOrNull { position(shower, location, it).altitude }
                ?: return null
            if (position(shower, location, peakTime).altitude < 0) {
                return null
            }
            return RiseSetTransitTimes(night.start, peakTime, night.end)
        }
        val intersection = Range(rise ?: night.start, set ?: night.end).intersection(night) ?: return null
        return RiseSetTransitTimes(intersection.start, intersection.clamp(transit), intersection.end)
    }

    private val PEAK_SEARCH_STEP = 5.minutes
}
