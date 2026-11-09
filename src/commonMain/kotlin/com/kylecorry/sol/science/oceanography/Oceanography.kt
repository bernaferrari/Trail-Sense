package com.kylecorry.sol.science.oceanography

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.optimization.IExtremaFinder
import com.kylecorry.sol.math.optimization.NoisyExtremaFinder
import com.kylecorry.sol.science.astronomy.MoonCalculationModel
import com.kylecorry.sol.science.astronomy.locators.MoonPhaseModel
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.science.oceanography.waterlevel.IWaterLevelCalculator
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

object Oceanography {
    const val DENSITY_SALT_WATER = 1023.6f
    const val DENSITY_FRESH_WATER = 997.0474f

    private val LUNAR_HALF_DAY = 12.hours

    /**
     * Gets the depth below the water surface from the water pressure.
     */
    fun getDepth(pressure: Pressure, seaLevelPressure: Pressure, isSaltWater: Boolean = true): Distance {
        if (pressure <= seaLevelPressure) {
            return Distance.from(0f, DistanceUnits.Meters)
        }

        val density = if (isSaltWater) DENSITY_SALT_WATER else DENSITY_FRESH_WATER
        val hpa = pressure.convertTo(PressureUnits.Hpa).value
        val seaLevelHpa = seaLevelPressure.convertTo(PressureUnits.Hpa).value
        val pascals = (hpa - seaLevelHpa) * 100
        return Distance.from(pascals / (Geophysics.GRAVITY * density), DistanceUnits.Meters)
    }

    /**
     * Estimates the tidal range from the moon phase over the last few days.
     */
    fun getTidalRange(time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): TidalRange {
        val phaseModel = MoonPhaseModel()
        for (daysAgo in 0..3) {
            val phase = phaseModel.getPhase(time.plus(-daysAgo, DateTimeUnit.DAY, timeZone)).phase
            when (phase) {
                MoonTruePhase.New, MoonTruePhase.Full -> return TidalRange.Spring
                MoonTruePhase.FirstQuarter, MoonTruePhase.ThirdQuarter -> return TidalRange.Neap
                else -> Unit
            }
        }
        return TidalRange.Normal
    }

    /**
     * Gets the high and low tides between [start] and [end].
     */
    fun getTides(
        calculator: IWaterLevelCalculator,
        start: Instant,
        end: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        extremaFinder: IExtremaFinder = NoisyExtremaFinder(1.0, 10)
    ): List<Tide> {
        val minutes = (end - start).inWholeMinutes
        val extrema = extremaFinder.find(Range(0.0, minutes.toDouble())) {
            calculator.calculate(start + it.toLong().minutes, timeZone).toDouble()
        }
        return extrema.map {
            Tide(start + it.point.x.toLong().minutes, it.isHigh, it.point.y, timeZone)
        }
    }

    /**
     * Gets the time between the moon's last transit (upper or lower) and the high tide.
     */
    fun getLunitidalInterval(
        highTideTime: Instant,
        location: Coordinate = Coordinate.zero,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Duration? {
        val lastTransit = listOfNotNull(
            lastMoonTransit(location, highTideTime, timeZone),
            lastMoonTransit(location.antipode, highTideTime, timeZone)
        ).maxOrNull() ?: return null

        var interval = highTideTime - lastTransit
        while (interval > LUNAR_HALF_DAY) {
            interval -= LUNAR_HALF_DAY
        }
        return interval
    }

    /**
     * Gets the average lunitidal interval of the high tides, accounting for intervals that wrap around
     * the 12 hour lunar half day.
     */
    fun getMeanLunitidalInterval(
        highTideTimes: List<Instant>,
        location: Coordinate = Coordinate.zero,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Duration? {
        val intervals = highTideTimes.mapNotNull { getLunitidalInterval(it, location, timeZone) }
        if (intervals.isEmpty()) {
            return null
        }

        val wrapsAround = intervals.any { it > 8.hours } && intervals.any { it < 4.hours }
        val total = intervals.fold(Duration.ZERO) { sum, interval ->
            sum + if (wrapsAround && interval < 4.hours) interval + LUNAR_HALF_DAY else interval
        }

        var average = total / intervals.size
        while (average > LUNAR_HALF_DAY) {
            average -= LUNAR_HALF_DAY
        }
        return average
    }

    private fun lastMoonTransit(location: Coordinate, time: Instant, zone: TimeZone): Instant? {
        return (-1..1)
            .mapNotNull {
                val day = time.plus(it, DateTimeUnit.DAY, zone)
                MoonCalculationModel.events(day, zone, location, false, false).transit
            }
            .filter { it < time }
            .maxOrNull()
    }
}
