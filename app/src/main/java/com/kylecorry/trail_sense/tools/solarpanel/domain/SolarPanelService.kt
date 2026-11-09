package com.kylecorry.trail_sense.tools.solarpanel.domain

import kotlin.time.Duration.Companion.seconds

import kotlin.time.Instant

import com.kylecorry.sol.time.Time

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.calculus.Calculus
import com.kylecorry.sol.math.optimization.HillClimbingOptimizer
import com.kylecorry.sol.math.optimization.SimulatedAnnealingOptimizer
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlin.time.Duration
import kotlinx.datetime.TimeZone
import com.kylecorry.trail_sense.shared.time.atEndOfDay
import com.kylecorry.trail_sense.shared.time.toLocalDate

class SolarPanelService(
    private val timeProvider: ZonedDateTimeProvider = SystemZonedDateTimeProvider()
) {

    /**
     * Gets the solar radiation in kWh / m^2
     */
    fun getSolarEnergy(
        location: Coordinate,
        tilt: Float,
        azimuth: Bearing,
        duration: Duration = 1.days,
        restrictToToday: Boolean = false
    ): Float {
        val now = timeProvider.getTime()
        val time = now.instant
        var end = time + duration
        if (end.toLocalDate(now.timeZone) != time.toLocalDate(now.timeZone) && restrictToToday) {
            end = time.atEndOfDay(now.timeZone)
        }
        return getSolarRadiation(
            time,
            end,
            location,
            tilt,
            azimuth
        ).toFloat().coerceAtLeast(0f)
    }

    fun getBestPosition(
        location: Coordinate,
        maxDuration: Duration,
        restrictToToday: Boolean = false
    ): Pair<Float, Bearing> {
        val duration = if (maxDuration <= 15.minutes + 5.seconds) {
            15.minutes + 15.seconds
        } else {
            maxDuration
        }

        return getBestPosition(
            location,
            maxDuration = duration,
            energyResolution = if (duration < 6.hours) 15.minutes else 30.minutes,
            restrictToToday = restrictToToday
        )
    }

    private fun getSolarRadiation(
        start: Instant,
        end: Instant,
        location: Coordinate,
        tilt: Float,
        bearing: Bearing,
        dt: Duration = 15.minutes,
    ): Double {
        val secondsToHours = 1.0 / (60 * 60)
        val original = Calculus.integral(
            0.0,
            (end - start).inWholeSeconds * secondsToHours,
            dt.inWholeSeconds * secondsToHours
        ) { hours ->
            val t = start + Time.hours(hours)
            Astronomy.getSolarRadiation(t, location, tilt, bearing, withRefraction = true)
                .coerceAtLeast(0.0)
        }

        if (original > 0) {
            return original
        }

        // Try again, but allowing negative values
        return Calculus.integral(
            0.0,
            (end - start).inWholeSeconds * secondsToHours,
            dt.inWholeSeconds * secondsToHours
        ) { hours ->
            val t = start + Time.hours(hours)
            Astronomy.getSolarRadiation(t, location, tilt, bearing, withRefraction = true)
        }
    }

    private fun getBestPosition(
        location: Coordinate,
        start: Instant = timeProvider.getTime().instant,
        maxDuration: Duration = 1.days,
        energyResolution: Duration = 30.minutes,
        restrictToToday: Boolean = false,
        zone: TimeZone = timeProvider.getTime().timeZone
    ): Pair<Float, Bearing> {
        var end = start + maxDuration
        if (end.toLocalDate(zone) != start.toLocalDate(zone) && restrictToToday) {
            end = start.atEndOfDay(zone)
        }

        val startAzimuth = if (location.isNorthernHemisphere) {
            // East
            80.0
        } else {
            // West
            -100.0
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            // West
            280.0
        } else {
            // East
            100.0
        }

        val startTilt = 0.0
        val endTilt = 90.0

        val fn = { x: Double, y: Double ->
            getSolarRadiation(
                start,
                end,
                location,
                y.toFloat(),
                Bearing.from(x.toFloat()),
                energyResolution
            )
        }

        val optimizer = if (start.toLocalDate(zone) == end.toLocalDate(zone)) {
            HillClimbingOptimizer(
                1.0,
                2000
            )
        } else {
            // When crossing days, hill climbing can get stuck in a local maximum
            SimulatedAnnealingOptimizer(
                10.0,
                1.0,
                2000
            )
        }

        val best = optimizer.optimize(
            Range(startAzimuth, endAzimuth),
            Range(startTilt, endTilt),
            true,
            fn = fn
        )

        return Pair(best.second.toFloat(), Bearing.from(best.first.toFloat()))
    }
}
