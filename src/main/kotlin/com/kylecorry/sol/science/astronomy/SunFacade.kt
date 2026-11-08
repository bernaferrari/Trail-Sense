package com.kylecorry.sol.science.astronomy

import kotlin.time.toKotlinInstant
import kotlin.time.toJavaInstant
import kotlinx.datetime.toKotlinTimeZone
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.locators.Sun
import com.kylecorry.sol.science.astronomy.sun.SolarRadiationCalculator
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.toJulianCenturies
import com.kylecorry.sol.science.astronomy.units.fromJulianDay
import com.kylecorry.sol.science.astronomy.units.toJulianDay
import com.kylecorry.sol.science.astronomy.units.toLocal
import com.kylecorry.sol.science.astronomy.units.toUniversalTime
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.atStartOfDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

internal object SunFacade {

    private val sun = Sun()
    private val radiation = SolarRadiationCalculator()

    fun getSunEvents(
        date: ZonedDateTime,
        location: Coordinate,
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {

        val result = SunCalculationModel.events(
            date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone(), location, mode,
            withRefraction, withParallax
        )
        return RiseSetTransitTimes(
            result.rise?.toJavaInstant()?.atZone(date.zone),
            result.transit?.toJavaInstant()?.atZone(date.zone),
            result.set?.toJavaInstant()?.atZone(date.zone)
        )
    }

    fun getSunPosition(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return SunCalculationModel.position(time.toInstant().toKotlinInstant(), location, withRefraction, withParallax)
    }

    fun getNextSunset(
        time: ZonedDateTime,
        location: Coordinate,
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): ZonedDateTime? {
        val today = getSunEvents(time, location, mode, withRefraction, withParallax)
        if (today.set != null && today.set > time) {
            return today.set
        }

        val tomorrow = getSunEvents(time.plusDays(1), location, mode, withRefraction, withParallax)
        if (tomorrow.set != null && tomorrow.set > time) {
            return tomorrow.set
        }

        return null
    }

    fun getNextSunrise(
        time: ZonedDateTime,
        location: Coordinate,
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): ZonedDateTime? {
        val today = getSunEvents(time, location, mode, withRefraction, withParallax)
        if (today.rise != null && today.rise > time) {
            return today.rise
        }

        val tomorrow = getSunEvents(time.plusDays(1), location, mode, withRefraction, withParallax)
        if (tomorrow.rise != null && tomorrow.rise > time) {
            return tomorrow.rise
        }

        return null
    }

    fun isSunUp(
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Boolean {
        return getSunPosition(time, location, withRefraction, withParallax).altitude > 0
    }

    fun getDaylightLength(
        date: ZonedDateTime,
        location: Coordinate,
        sunTimesMode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Duration {
        val startOfDay = date.atStartOfDay()
        val sunrise =
            getNextSunrise(startOfDay, location, sunTimesMode, withRefraction, withParallax)
        val sunset = getNextSunset(startOfDay, location, sunTimesMode, withRefraction, withParallax)

        if (sunrise != null && sunset != null && sunset > sunrise) {
            // Rise in morning, set at night
            return Duration.between(sunrise, sunset)
        } else if (sunrise == null && sunset == null) {
            // Sun doesn't rise or set
            return if (isSunUp(
                    startOfDay,
                    location,
                    withRefraction,
                    withParallax
                )
            ) Duration.between(
                startOfDay,
                startOfDay.plusDays(1)
            ) else Duration.ZERO
        } else if (sunrise != null && sunset == null) {
            // Sun rises but doesn't set
            return Duration.between(sunrise, startOfDay.plusDays(1))
        } else if (sunrise == null) {
            // Sun sets but doesn't rise
            return Duration.between(startOfDay, sunset)
        } else {
            // Sun sets in morning, rises at night
            return Duration.between(startOfDay, sunset)
                .plus(Duration.between(sunrise, startOfDay.plusDays(1)))
        }
    }

    /**
     * Gets the solar radiation for the given time in kW/m^2
     */
    fun getSolarRadiation(
        date: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Double {
        return radiation.getRadiation(
            date.toInstant().toKotlinInstant(),
            location,
            withRefraction = withRefraction,
            withParallax = withParallax
        )
    }

    /**
     * Gets the solar radiation for the given time in kW/m^2
     */
    fun getSolarRadiation(
        date: ZonedDateTime,
        location: Coordinate,
        tilt: Float,
        azimuth: Bearing,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Double {
        return radiation.getRadiation(
            date.toInstant().toKotlinInstant(),
            location,
            tilt,
            azimuth,
            withRefraction,
            withParallax
        )
    }

    /**
     * Gets the times the sun is above the horizon within approximately a day.
     * If the sun does not set, it will return from the last rise (or start of day) until the end of the day.
     * @param location The location of the observer
     * @param time The current time
     * @param nextRiseOffset The duration before the next rise to switch to the next day's times
     * @param mode The mode to use for calculating sun times
     * @param withRefraction True to correct for atmospheric refraction
     * @param withParallax True to correct for parallax
     * @return The range of times the sun is above the horizon or null if it is not above the horizon
     * within approximately a day.
     */
    fun getSunAboveHorizonTimes(
        location: Coordinate,
        time: ZonedDateTime,
        nextRiseOffset: Duration = Duration.ofHours(6),
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Range<ZonedDateTime>? {
        return AboveHorizonFacade.getAboveHorizonTimes(
            location,
            time,
            nextRiseOffset,
            { loc, t -> isSunUp(t, loc, withRefraction, withParallax) },
            { loc, t -> getSunEvents(t, loc, mode, withRefraction, withParallax) }
        )
    }

    fun getClosestNight(
        time: ZonedDateTime?,
        location: Coordinate,
        sunTimesMode: SunTimesMode
    ): Range<ZonedDateTime>? {
        if (time == null) return null
        val night = SunCalculationModel.closestNight(
            time.toInstant().toKotlinInstant(), time.zone.toKotlinTimeZone(), location, sunTimesMode
        ) ?: return null
        return Range(night.start.toJavaInstant().atZone(time.zone), night.end.toJavaInstant().atZone(time.zone))
    }

    fun getNextTimeAtSolarLongitude(longitude: Float, today: ZonedDateTime): ZonedDateTime {
        return SunCalculationModel.nextAtLongitude(
            longitude, today.toInstant().toKotlinInstant(), today.zone.toKotlinTimeZone()
        ).toJavaInstant().atZone(today.zone)
    }

    fun getSolarLongitude(date: ZonedDateTime): Float {
        return SunCalculationModel.longitude(date.toInstant().toKotlinInstant())
    }
}
