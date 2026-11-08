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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object Oceanography {
    const val DENSITY_SALT_WATER = 1023.6f
    const val DENSITY_FRESH_WATER = 997.0474f

    fun getDepth(pressure: Pressure, seaLevelPressure: Pressure, isSaltWater: Boolean = true): Distance {
        if (pressure <= seaLevelPressure) return Distance.from(0f, DistanceUnits.Meters)
        val density = if (isSaltWater) DENSITY_SALT_WATER else DENSITY_FRESH_WATER
        val difference = pressure.convertTo(PressureUnits.Hpa).value - seaLevelPressure.convertTo(PressureUnits.Hpa).value
        return Distance.from(difference * 100 / (Geophysics.GRAVITY * density), DistanceUnits.Meters)
    }

    fun getTidalRange(time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): TidalRange {
        for (i in 0..3) {
            when (MoonPhaseModel().getPhase(time.plus(-i, DateTimeUnit.DAY, timeZone)).phase) {
                MoonTruePhase.New, MoonTruePhase.Full -> return TidalRange.Spring
                MoonTruePhase.FirstQuarter, MoonTruePhase.ThirdQuarter -> return TidalRange.Neap
                else -> Unit
            }
        }
        return TidalRange.Normal
    }

    fun getTides(
        calculator: IWaterLevelCalculator, start: Instant, end: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        extremaFinder: IExtremaFinder = NoisyExtremaFinder(1.0, 10)
    ): List<Tide> {
        val range = (end - start).inWholeMinutes
        return extremaFinder.find(Range(0.0, range.toDouble())) {
            calculator.calculate(start + it.toLong().minutes, timeZone).toDouble()
        }.map { Tide(start + it.point.x.toLong().minutes, it.isHigh, it.point.y, timeZone) }
    }

    fun getLunitidalInterval(
        highTideTime: Instant, location: Coordinate = Coordinate.zero,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Duration? {
        val lastTransit = listOfNotNull(
            lastMoonTransit(location, highTideTime, timeZone),
            lastMoonTransit(location.antipode, highTideTime, timeZone)
        ).maxOrNull() ?: return null
        var interval = highTideTime - lastTransit
        while (interval > 12.hours) interval -= 12.hours
        return interval
    }

    fun getMeanLunitidalInterval(
        highTideTimes: List<Instant>, location: Coordinate = Coordinate.zero,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Duration? {
        val intervals = highTideTimes.mapNotNull { getLunitidalInterval(it, location, timeZone) }
        if (intervals.isEmpty()) return null
        val correction = intervals.any { it > 8.hours } && intervals.any { it < 4.hours }
        var average = intervals.fold(Duration.ZERO) { sum, interval ->
            sum + if (correction && interval < 4.hours) interval + 12.hours else interval
        } / intervals.size
        while (average > 12.hours) average -= 12.hours
        return average
    }

    private fun lastMoonTransit(location: Coordinate, time: Instant, zone: TimeZone): Instant? =
        (-1..1).mapNotNull {
            MoonCalculationModel.events(time.plus(it, DateTimeUnit.DAY, zone), zone, location, false, false).transit
        }.filter { it < time }.maxOrNull()
}
