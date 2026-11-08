package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.locators.Moon
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.toUniversalTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal object MoonFacade {
    private val moon = Moon()

    fun getMoonEvents(
        date: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {
        val times = MoonCalculationModel.events(
            date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone(), location, withRefraction, withParallax
        )
        return RiseSetTransitTimes(
            times.rise?.toJavaInstant()?.atZone(date.zone), times.transit?.toJavaInstant()?.atZone(date.zone),
            times.set?.toJavaInstant()?.atZone(date.zone)
        )
    }

    fun getMoonAltitude(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Float {
        return JvmAstroUtils.getAltitude(
            moon,
            time.toUniversalTime(),
            location,
            withRefraction,
            withParallax
        )
    }

    fun getMoonPosition(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return MoonCalculationModel.position(time.toInstant().toKotlinInstant(), location, withRefraction, withParallax)
    }

    fun getNextMoonset(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): ZonedDateTime? {
        val today = getMoonEvents(time, location, withRefraction, withParallax)
        if (today.set != null && today.set > time) {
            return today.set
        }

        val tomorrow = getMoonEvents(time.plusDays(1), location, withRefraction, withParallax)
        if (tomorrow.set != null && tomorrow.set > time) {
            return tomorrow.set
        }

        return null
    }

    fun getNextMoonrise(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): ZonedDateTime? {
        val today = getMoonEvents(time, location, withRefraction, withParallax)
        if (today.rise != null && today.rise > time) {
            return today.rise
        }

        val tomorrow = getMoonEvents(time.plusDays(1), location, withRefraction, withParallax)
        if (tomorrow.rise != null && tomorrow.rise > time) {
            return tomorrow.rise
        }

        return null
    }

    fun getMoonPhase(date: ZonedDateTime): MoonPhase {
        return moon.getPhase(date.toUniversalTime())
    }

    fun isMoonUp(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Boolean {
        return getMoonPosition(time, location, withRefraction, withParallax).altitude > 0
    }

    private fun getMoonDistance(time: ZonedDateTime): Distance {
        return moon.getDistance(time.toUniversalTime())
    }

    fun isSuperMoon(time: ZonedDateTime): Boolean {
        val phase = getMoonPhase(time)
        if (phase.phase != MoonTruePhase.Full) {
            return false
        }
        val distance = getMoonDistance(time)
        return distance.convertTo(DistanceUnits.Kilometers).value <= 360000f
    }

    /**
     * Gets the times the moon is above the horizon within approximately a day.
     * If the sun does not set, it will return from the last rise (or start of day) until the end of the day.
     * @param location The location of the observer
     * @param time The current time
     * @param nextRiseOffset The duration before the next rise to switch to the next day's times
     * @param withRefraction True to correct for atmospheric refraction
     * @param withParallax True to correct for parallax
     * @return The range of times the moon is above the horizon or null if it is not above the horizon
     * within approximately a day.
     */
    fun getMoonAboveHorizonTimes(
        location: Coordinate,
        time: ZonedDateTime,
        nextRiseOffset: Duration = Duration.ofHours(6),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Range<ZonedDateTime>? {
        return AboveHorizonFacade.getAboveHorizonTimes(
            location,
            time,
            nextRiseOffset,
            { loc, t -> isMoonUp(t, loc, withRefraction, withParallax) },
            { loc, t -> getMoonEvents(t, loc, withRefraction, withParallax) }
        )
    }

    /**
     * The position angle of the midpoint of the Moon's illuminated limb relative to the
     * observer's local zenith, in degrees. Positive angles are eastward (counterclockwise in an
     * upright, non-mirrored view of the sky).
     */
    fun getZenithAngleOfBrightLimb(time: ZonedDateTime, location: Coordinate): Float {
        return moon.getZenithAngleOfBrightLimb(time.toUniversalTime(), location)
    }

    /**
     * The position angle of the midpoint of the Moon's illuminated limb relative to the
     * moon's North Pole, in degrees. Positive angles are eastward (counterclockwise in an
     * upright, non-mirrored view of the sky).
     */
    fun getPositionAngleOfBrightLimb(time: ZonedDateTime): Float {
        return moon.getPositionAngleOfBrightLimb(time.toUniversalTime()).toFloat()
    }

    /**
     * The parallactic angle of the moon in degrees.
     */
    fun getMoonParallacticAngle(time: ZonedDateTime, location: Coordinate): Float {
        return JvmAstroUtils.getParallacticAngle(moon, time.toUniversalTime(), location)
    }
}
