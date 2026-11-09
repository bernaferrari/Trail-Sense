@file:Suppress("MaxLineLength", "ReturnCount")

package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.eclipse.Eclipse
import com.kylecorry.sol.science.astronomy.eclipse.EclipseType
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.eclipse.lunar.LunarEclipseModel
import com.kylecorry.sol.science.astronomy.eclipse.lunar.LunarEclipseShadowModel
import com.kylecorry.sol.science.astronomy.eclipse.solar.SolarEclipseModel
import com.kylecorry.sol.science.astronomy.locators.MoonPhaseModel
import com.kylecorry.sol.science.astronomy.locators.MoonPositionModel
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.locators.StarPositionModel
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.astronomy.navigation.StarIdentification
import com.kylecorry.sol.science.astronomy.navigation.StarIdentifierSettings
import com.kylecorry.sol.science.astronomy.rst.SearchRiseSetTimes
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.science.astronomy.stars.StarLocationCalculator
import com.kylecorry.sol.science.astronomy.stars.PlateSolver
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.science.astronomy.sun.SolarRadiationCalculator
import com.kylecorry.sol.science.geophysics.VerticalDeflectionModel
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

object Astronomy {
    fun getSunEvents(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                     mode: SunTimesMode = SunTimesMode.Actual, withRefraction: Boolean = false,
                     withParallax: Boolean = false): RiseSetTransitTimes =
        SunCalculationModel.events(time, timeZone, location, mode, withRefraction, withParallax).public()

    fun getSunPosition(time: Instant, location: Coordinate, withRefraction: Boolean = false, withParallax: Boolean = false) =
        SunCalculationModel.position(time, location, withRefraction, withParallax)

    fun isSunUp(time: Instant, location: Coordinate, withRefraction: Boolean = false, withParallax: Boolean = false) =
        getSunPosition(time, location, withRefraction, withParallax).altitude > 0

    fun getSolarRadiation(time: Instant, location: Coordinate, withRefraction: Boolean = false,
                          withParallax: Boolean = false) =
        SolarRadiationCalculator().getRadiation(time, location, withRefraction = withRefraction, withParallax = withParallax)

    fun getSolarRadiation(time: Instant, location: Coordinate, tilt: Float, azimuth: Bearing,
                          withRefraction: Boolean = false, withParallax: Boolean = false) =
        SolarRadiationCalculator().getRadiation(time, location, tilt, azimuth, withRefraction, withParallax)

    fun getMoonEvents(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                      withRefraction: Boolean = false, withParallax: Boolean = false): RiseSetTransitTimes =
        MoonCalculationModel.events(time, timeZone, location, withRefraction, withParallax).public()

    fun getMoonPosition(time: Instant, location: Coordinate, withRefraction: Boolean = false, withParallax: Boolean = false) =
        MoonCalculationModel.position(time, location, withRefraction, withParallax)

    fun getMoonPhase(time: Instant) = MoonPhaseModel().getPhase(time)
    fun isMoonUp(time: Instant, location: Coordinate, withRefraction: Boolean = false, withParallax: Boolean = false) =
        getMoonPosition(time, location, withRefraction, withParallax).altitude > 0
    fun isSuperMoon(time: Instant) = getMoonPhase(time).phase == MoonTruePhase.Full &&
        MoonPositionModel().getDistance(time).convertTo(DistanceUnits.Kilometers).value <= 360000f
    fun getMoonZenithAngleOfBrightLimb(time: Instant, location: Coordinate) =
        MoonPhaseModel().getZenithAngleOfBrightLimb(time, location)
    fun getMoonPositionAngleOfBrightLimb(time: Instant) = MoonPhaseModel().getPositionAngleOfBrightLimb(time).toFloat()
    fun getMoonParallacticAngle(time: Instant, location: Coordinate) =
        AstroUtils.getParallacticAngle(MoonPositionModel(), time, location)
    fun getSeason(location: Coordinate, time: Instant) = SeasonCalculator.getSeason(location, time)

    fun getPlanetPosition(planet: Planet, time: Instant, location: Coordinate,
                          withRefraction: Boolean = false, withParallax: Boolean = false) =
        PlanetCalculationModel.position(planet, time, location, withRefraction, withParallax)
    fun getPlanetEvents(planet: Planet, time: Instant, location: Coordinate,
                        timeZone: TimeZone = TimeZone.currentSystemDefault(), withRefraction: Boolean = false,
                        withParallax: Boolean = false): RiseSetTransitTimes =
        PlanetCalculationModel.events(planet, time, timeZone, location, withRefraction, withParallax).public()

    /**
     * Finds the next eclipse visible from [location] that is in progress or starts after [time].
     * [timeZone] determines the local days used when searching for lunar eclipses.
     */
    fun getNextEclipse(time: Instant, location: Coordinate, type: EclipseType, maxSearch: Duration? = null,
                       timeZone: TimeZone = TimeZone.currentSystemDefault()): Eclipse? {
        val result = when (type) {
            EclipseType.PartialLunar -> LunarEclipseModel(false).next(time, location, timeZone)
            EclipseType.TotalLunar -> LunarEclipseModel(true).next(time, location, timeZone)
            EclipseType.Solar -> SolarEclipseModel(maxDuration = maxSearch).getNextEclipse(time, location)
        } ?: return null
        return Eclipse(result.start, result.end, result.magnitude, result.obscuration, result.maximum)
    }
    fun getEclipseMagnitude(time: Instant, location: Coordinate, type: EclipseType): Float? =
        if (type == EclipseType.Solar) SolarEclipseModel().getMagnitude(time, location) else null
    fun getEclipseObscuration(time: Instant, location: Coordinate, type: EclipseType): Float? =
        if (type == EclipseType.Solar) SolarEclipseModel().getObscuration(time, location) else null
    fun getLunarEclipseShadow(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                              withRefraction: Boolean = false, withParallax: Boolean = false): LunarEclipseShadow =
        LunarEclipseShadowModel().getPeakShadow(time, timeZone, location, withRefraction, withParallax)

    fun getMeteorShower(location: Coordinate, time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()) =
        MeteorModel.shower(location, time, timeZone)?.public()
    fun getActiveMeteorShowers(location: Coordinate, time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()) =
        MeteorModel.active(location, time, timeZone).map { it.public() }
    fun getMeteorShowerPosition(shower: MeteorShower, location: Coordinate, time: Instant) =
        MeteorModel.position(shower, location, time)
    fun getStarPosition(star: Star, time: Instant, location: Coordinate, withRefraction: Boolean = false):
        com.kylecorry.sol.science.astronomy.units.CelestialObservation {
        val position = AstroUtils.getLocation(StarPositionModel(star), time, location, withRefraction)
        return com.kylecorry.sol.science.astronomy.units.CelestialObservation(
            Bearing.from(position.azimuth.toFloat()), position.altitude.toFloat(), visualMagnitude = star.magnitude
        )
    }
    fun getColorTemperature(star: Star) = 4600f * ((1 / (0.92f * star.colorIndexBV + 1.7f)) +
        (1 / (0.92f * star.colorIndexBV + 0.62f)))
    fun getZenithDistance(altitude: Float) = Distance.nauticalMiles((90f - altitude) * 60)
    fun getLocationFromStars(starReadings: List<StarReading>, approximateLocation: Coordinate? = null): Coordinate? =
        StarLocationCalculator().getLocationFromStars(starReadings, approximateLocation)
    fun identifyStars(image: SkyImage, approximateLocation: Coordinate,
                      settings: StarIdentifierSettings = StarIdentifierSettings()): StarIdentification? =
        CelestialNavigationFacade.identifyStars(image, approximateLocation, settings)
    fun getLocationFromStars(image: SkyImage, approximateLocation: Coordinate,
                             settings: StarIdentifierSettings = StarIdentifierSettings(),
                             deflection: VerticalDeflectionModel? = null): Coordinate? =
        CelestialNavigationFacade.getLocationFromStars(image, approximateLocation, settings, deflection)
    fun getOrientationFromStars(image: SkyImage, location: Coordinate,
                                settings: StarIdentifierSettings = StarIdentifierSettings(maxZenithOffsetDegrees = 5.0)) =
        CelestialNavigationFacade.getOrientationFromStars(image, location, settings)


    fun getNextSunrise(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                       mode: SunTimesMode = SunTimesMode.Actual, withRefraction: Boolean = false,
                       withParallax: Boolean = false): Instant? = nextEvent(time, timeZone, true) {
        getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
    }
    fun getNextSunset(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                      mode: SunTimesMode = SunTimesMode.Actual, withRefraction: Boolean = false,
                      withParallax: Boolean = false): Instant? = nextEvent(time, timeZone, false) {
        getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
    }
    fun getNextMoonrise(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                        withRefraction: Boolean = false, withParallax: Boolean = false): Instant? =
        nextEvent(time, timeZone, true) { getMoonEvents(it, location, timeZone, withRefraction, withParallax) }
    fun getNextMoonset(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                       withRefraction: Boolean = false, withParallax: Boolean = false): Instant? =
        nextEvent(time, timeZone, false) { getMoonEvents(it, location, timeZone, withRefraction, withParallax) }

    fun getDaylightLength(time: Instant, location: Coordinate, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                          sunTimesMode: SunTimesMode = SunTimesMode.Actual, withRefraction: Boolean = false,
                          withParallax: Boolean = false): Duration {
        val start = time.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone)
        val end = start.plus(1, DateTimeUnit.DAY, timeZone)
        val rise = getNextSunrise(start, location, timeZone, sunTimesMode, withRefraction, withParallax)
        val set = getNextSunset(start, location, timeZone, sunTimesMode, withRefraction, withParallax)
        return when {
            rise != null && set != null && set > rise -> set - rise
            rise == null && set == null -> if (isSunUp(start, location, withRefraction, withParallax)) end - start else Duration.ZERO
            rise != null && set == null -> end - rise
            rise == null -> checkNotNull(set) - start
            else -> checkNotNull(set) - start + (end - rise)
        }
    }

    fun getSunAboveHorizonTimes(location: Coordinate, time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                                nextRiseOffset: Duration = 6.hours, mode: SunTimesMode = SunTimesMode.Actual,
                                withRefraction: Boolean = false, withParallax: Boolean = false): Range<Instant>? =
        aboveHorizon(time, timeZone, nextRiseOffset, isSunUp(time, location, withRefraction, withParallax)) {
            getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
        }
    fun getMoonAboveHorizonTimes(location: Coordinate, time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault(),
                                 nextRiseOffset: Duration = 6.hours, withRefraction: Boolean = false,
                                 withParallax: Boolean = false): Range<Instant>? =
        aboveHorizon(time, timeZone, nextRiseOffset, isMoonUp(time, location, withRefraction, withParallax)) {
            getMoonEvents(it, location, timeZone, withRefraction, withParallax)
        }

    fun plateSolve(readings: List<CelestialObservation>, time: Instant, approximateLocation: Coordinate? = null,
                   timeZone: TimeZone = TimeZone.currentSystemDefault(), tolerance: Float = 0.04f,
                   minMatches: Int = 5, minMagnitude: Float = 4f) = PlateSolver(
        tolerance, minMatches, minMagnitude, if (approximateLocation == null) PlateSolver.UNKNOWN_LOCATION_ZENITH_OFFSET
        else PlateSolver.DEFAULT_MAX_ZENITH_OFFSET
    ).solve(readings, time, approximateLocation ?: Time.getLocationFromTimeZone(timeZone))

    private fun nextEvent(time: Instant, zone: TimeZone, rise: Boolean,
                          events: (Instant) -> RiseSetTransitTimes): Instant? {
        for (day in 0..1) {
            val result = events(time.plus(day, DateTimeUnit.DAY, zone))
            val event = if (rise) result.rise else result.set
            if (event != null && event > time) return event
        }
        return null
    }

    private fun aboveHorizon(time: Instant, zone: TimeZone, offset: Duration, isUp: Boolean,
                             events: (Instant) -> RiseSetTransitTimes): Range<Instant>? {
        val results = (-1..1).map { events(time.plus(it, DateTimeUnit.DAY, zone)) }
        val rises = results.mapNotNull { it.rise }
        val sets = results.mapNotNull { it.set }
        val lastRise = rises.filter { it < time }.maxOrNull()
        val nextRise = rises.filter { it > time }.minOrNull()
        val lastSet = sets.filter { it < time }.maxOrNull()
        val nextSet = sets.filter { it > time }.minOrNull()
        val date = time.toLocalDateTime(zone).date
        val start = date.atStartOfDayIn(zone)
        val end = LocalDateTime(date.year, date.month, date.day, 23, 59, 59, 999999999).toInstant(zone)
        if (isUp) return Range(lastRise ?: start, nextSet ?: end)
        if (nextRise == null || nextRise - time > offset) {
            if (lastRise == null && lastSet == null) return null
            return Range(lastRise ?: start, lastSet ?: end)
        }
        return Range(nextRise, nextSet ?: end)
    }

    private fun SearchRiseSetTimes.public() = RiseSetTransitTimes(rise, transit, set)
    private fun MeteorPeak.public() = MeteorShowerPeak(shower, start, peak, end)
}
