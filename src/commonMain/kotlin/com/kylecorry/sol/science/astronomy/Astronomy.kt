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
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.astronomy.navigation.StarIdentification
import com.kylecorry.sol.science.astronomy.navigation.StarIdentifierSettings
import com.kylecorry.sol.science.astronomy.stars.DetectedStar
import com.kylecorry.sol.science.astronomy.stars.PlateSolver
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.stars.StarLocationCalculator
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.science.astronomy.sun.SolarRadiationCalculator
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.geophysics.VerticalDeflectionModel
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.atEndOfDayIn
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Entry point for astronomical calculations.
 *
 * Every calculation takes a [kotlin.time.Instant]. Calculations that depend on a calendar day
 * (rise and set times, daylight length, meteor shower peaks, lunar eclipse searches) also take the
 * observer's [TimeZone], which defaults to the system time zone and decides where each day starts.
 */
object Astronomy {

    // region Sun

    /**
     * Gets the sunrise, solar noon, and sunset on the local day containing [time].
     * @param mode the altitude that counts as the sun rising or setting (actual, civil, nautical, astronomical)
     */
    fun getSunEvents(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {
        return SunCalculationModel.events(time, timeZone, location, mode, withRefraction, withParallax)
    }

    /**
     * Get the position of the sun at a given time and location.
     * @param time the observer's time
     * @param location the observer's location
     * @param withRefraction whether to factor in refraction
     * @param withParallax whether to factor in parallax
     * @return a celestial observation containing an altitude, azimuth,
     * angular diameter (won't be null), and distance (won't be null)
     */
    fun getSunPosition(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return SunCalculationModel.position(time, location, withRefraction, withParallax)
    }

    /**
     * Gets the first sunset after [time], searching the current and next local day.
     */
    fun getNextSunset(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Instant? {
        return nextEvent(time, timeZone, RiseSetTransitTimes::set) {
            getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
        }
    }

    /**
     * Gets the first sunrise after [time], searching the current and next local day.
     */
    fun getNextSunrise(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Instant? {
        return nextEvent(time, timeZone, RiseSetTransitTimes::rise) {
            getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
        }
    }

    fun isSunUp(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Boolean {
        return getSunPosition(time, location, withRefraction, withParallax).altitude > 0
    }

    /**
     * Gets how long the sun is above the horizon during the local day containing [time].
     */
    fun getDaylightLength(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        sunTimesMode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Duration {
        val startOfDay = time.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone)
        val startOfNextDay = startOfDay.plus(1, DateTimeUnit.DAY, timeZone)
        val rise = getNextSunrise(startOfDay, location, timeZone, sunTimesMode, withRefraction, withParallax)
        val set = getNextSunset(startOfDay, location, timeZone, sunTimesMode, withRefraction, withParallax)

        return when {
            // Sun rises and then sets
            rise != null && set != null && set > rise -> set - rise
            // Sun is up all day or down all day
            rise == null && set == null -> if (isSunUp(startOfDay, location, withRefraction, withParallax)) {
                startOfNextDay - startOfDay
            } else {
                Duration.ZERO
            }
            // Sun rises but does not set
            set == null -> startOfNextDay - checkNotNull(rise)
            // Sun sets but does not rise
            rise == null -> set - startOfDay
            // Sun sets in the morning and rises again later
            else -> (set - startOfDay) + (startOfNextDay - rise)
        }
    }

    /**
     * Gets the solar radiation for the given time in kW/m^2
     */
    fun getSolarRadiation(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Double {
        return SolarRadiationCalculator().getRadiation(
            time,
            location,
            withRefraction = withRefraction,
            withParallax = withParallax
        )
    }

    /**
     * Gets the solar radiation for the given time in kW/m^2 on a surface with the given [tilt] and [azimuth]
     */
    @Suppress("LongParameterList")
    fun getSolarRadiation(
        time: Instant,
        location: Coordinate,
        tilt: Float,
        azimuth: Bearing,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Double {
        return SolarRadiationCalculator().getRadiation(time, location, tilt, azimuth, withRefraction, withParallax)
    }

    /**
     * Gets the times the sun is above the horizon within approximately a day.
     * If the sun does not set, it will return from the last rise (or start of day) until the end of the day.
     * @param location The location of the observer
     * @param time The current time
     * @param timeZone The time zone that determines the observer's day
     * @param nextRiseOffset The duration before the next rise to switch to the next day's times
     * @param mode The mode to use for calculating sun times
     * @param withRefraction True to correct for atmospheric refraction
     * @param withParallax True to correct for parallax
     * @return The range of times the sun is above the horizon or null if it is not above the horizon
     * within approximately a day.
     */
    @Suppress("LongParameterList")
    fun getSunAboveHorizonTimes(
        location: Coordinate,
        time: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        nextRiseOffset: Duration = 6.hours,
        mode: SunTimesMode = SunTimesMode.Actual,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Range<Instant>? {
        val isUp = isSunUp(time, location, withRefraction, withParallax)
        return aboveHorizonTimes(time, timeZone, nextRiseOffset, isUp) {
            getSunEvents(it, location, timeZone, mode, withRefraction, withParallax)
        }
    }

    // endregion

    // region Moon

    /**
     * Gets the moonrise, transit, and moonset on the local day containing [time].
     */
    fun getMoonEvents(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {
        return MoonCalculationModel.events(time, timeZone, location, withRefraction, withParallax)
    }

    /**
     * Get the position of the moon at a given time and location.
     * @param time the observer's time
     * @param location the observer's location
     * @param withRefraction whether to factor in refraction
     * @param withParallax whether to factor in parallax
     * @return a celestial observation containing an altitude, azimuth,
     * angular diameter (won't be null), and distance (won't be null)
     */
    fun getMoonPosition(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return MoonCalculationModel.position(time, location, withRefraction, withParallax)
    }

    /**
     * Gets the first moonset after [time], searching the current and next local day.
     */
    fun getNextMoonset(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Instant? {
        return nextEvent(time, timeZone, RiseSetTransitTimes::set) {
            getMoonEvents(it, location, timeZone, withRefraction, withParallax)
        }
    }

    /**
     * Gets the first moonrise after [time], searching the current and next local day.
     */
    fun getNextMoonrise(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Instant? {
        return nextEvent(time, timeZone, RiseSetTransitTimes::rise) {
            getMoonEvents(it, location, timeZone, withRefraction, withParallax)
        }
    }

    fun getMoonPhase(time: Instant): MoonPhase {
        return MoonPhaseModel().getPhase(time)
    }

    fun isMoonUp(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Boolean {
        return getMoonPosition(time, location, withRefraction, withParallax).altitude > 0
    }

    /**
     * Determines if the moon is a super moon: full and within 360,000 km of the earth.
     */
    fun isSuperMoon(time: Instant): Boolean {
        if (getMoonPhase(time).phase != MoonTruePhase.Full) {
            return false
        }
        val distance = MoonPositionModel().getDistance(time).convertTo(DistanceUnits.Kilometers).value
        return distance <= SUPER_MOON_MAX_DISTANCE_KM
    }

    /**
     * Gets the times the moon is above the horizon within approximately a day.
     * If the moon does not set, it will return from the last rise (or start of day) until the end of the day.
     * @param location The location of the observer
     * @param time The current time
     * @param timeZone The time zone that determines the observer's day
     * @param nextRiseOffset The duration before the next rise to switch to the next day's times
     * @param withRefraction True to correct for atmospheric refraction
     * @param withParallax True to correct for parallax
     * @return The range of times the moon is above the horizon or null if it is not above the horizon
     * within approximately a day.
     */
    fun getMoonAboveHorizonTimes(
        location: Coordinate,
        time: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        nextRiseOffset: Duration = 6.hours,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Range<Instant>? {
        val isUp = isMoonUp(time, location, withRefraction, withParallax)
        return aboveHorizonTimes(time, timeZone, nextRiseOffset, isUp) {
            getMoonEvents(it, location, timeZone, withRefraction, withParallax)
        }
    }

    /**
     * The position angle of the midpoint of the Moon's illuminated limb relative to the
     * observer's local zenith, in degrees. Positive angles are eastward (counterclockwise in an
     * upright, non-mirrored view of the sky).
     */
    fun getMoonZenithAngleOfBrightLimb(time: Instant, location: Coordinate): Float {
        return MoonPhaseModel().getZenithAngleOfBrightLimb(time, location)
    }

    /**
     * The position angle of the midpoint of the Moon's illuminated limb relative to the
     * moon's North Pole, in degrees. Positive angles are eastward (counterclockwise in an
     * upright, non-mirrored view of the sky).
     */
    fun getMoonPositionAngleOfBrightLimb(time: Instant): Float {
        return MoonPhaseModel().getPositionAngleOfBrightLimb(time).toFloat()
    }

    /**
     * The parallactic angle of the moon in degrees.
     */
    fun getMoonParallacticAngle(time: Instant, location: Coordinate): Float {
        return AstroUtils.getParallacticAngle(MoonPositionModel(), time, location)
    }

    // endregion

    fun getSeason(location: Coordinate, time: Instant): Season {
        return SeasonCalculator.getSeason(location, time)
    }

    // region Eclipses

    /**
     * Finds the next eclipse visible from [location] that is in progress or starts after [time].
     * @param maxSearch the maximum time to search for a solar eclipse, or null for the default
     * @param timeZone determines the local days used when searching for lunar eclipses
     */
    fun getNextEclipse(
        time: Instant,
        location: Coordinate,
        type: EclipseType,
        maxSearch: Duration? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Eclipse? {
        val result = when (type) {
            EclipseType.PartialLunar -> LunarEclipseModel(false).next(time, location, timeZone)
            EclipseType.TotalLunar -> LunarEclipseModel(true).next(time, location, timeZone)
            EclipseType.Solar -> SolarEclipseModel(maxDuration = maxSearch).getNextEclipse(time, location)
        } ?: return null
        return Eclipse(result.start, result.end, result.magnitude, result.obscuration, result.maximum)
    }

    /**
     * Gets the magnitude of the eclipse at [time], or null if it is not supported for the eclipse type.
     */
    fun getEclipseMagnitude(time: Instant, location: Coordinate, type: EclipseType): Float? {
        return if (type == EclipseType.Solar) SolarEclipseModel().getMagnitude(time, location) else null
    }

    /**
     * Gets the obscuration of the eclipse at [time], or null if it is not supported for the eclipse type.
     */
    fun getEclipseObscuration(time: Instant, location: Coordinate, type: EclipseType): Float? {
        return if (type == EclipseType.Solar) SolarEclipseModel().getObscuration(time, location) else null
    }

    /**
     * Gets the position and size of the earth's shadow during the peak of the lunar eclipse closest to [time].
     */
    fun getLunarEclipseShadow(
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): LunarEclipseShadow {
        return LunarEclipseShadowModel().getPeakShadow(time, timeZone, location, withRefraction, withParallax)
    }

    // endregion

    // region Meteor showers

    /**
     * Gets the meteor shower that peaks on the local day containing [time], if any.
     */
    fun getMeteorShower(
        location: Coordinate,
        time: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): MeteorShowerPeak? {
        return MeteorModel.shower(location, time, timeZone)
    }

    /**
     * Get a list of meteor showers which are active.
     * This does not check the time of day, so the shower may not currently be visible.
     */
    fun getActiveMeteorShowers(
        location: Coordinate,
        time: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<MeteorShowerPeak> {
        return MeteorModel.active(location, time, timeZone)
    }

    fun getMeteorShowerPosition(
        shower: MeteorShower,
        location: Coordinate,
        time: Instant
    ): CelestialObservation {
        return MeteorModel.position(shower, location, time)
    }

    // endregion

    // region Stars

    fun getStarPosition(
        star: Star,
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false
    ): CelestialObservation {
        val position = AstroUtils.getLocation(StarPositionModel(star), time, location, withRefraction)
        return CelestialObservation(
            Bearing.from(position.azimuth.toFloat()),
            position.altitude.toFloat(),
            visualMagnitude = star.magnitude
        )
    }

    /**
     * Get the color temperature of a star in Kelvin
     */
    fun getColorTemperature(star: Star): Float {
        // Ballesteros' formula
        val bv = star.colorIndexBV
        return 4600f * ((1 / (0.92f * bv + 1.7f)) + (1 / (0.92f * bv + 0.62f)))
    }

    /**
     * Matches the readings to stars. Readings are compared by the angles between them, so a constant
     * compass error doesn't matter.
     * @param readings the altitude and azimuth of each observed star
     * @param time the time the readings were taken
     * @param approximateLocation the location to within ~30° (the whole sky is searched if null)
     * @param timeZone used to estimate the location when [approximateLocation] is null
     * @param tolerance the fractional tolerance on the angular separation between readings
     * @param minMatches the minimum number of identified stars
     * @param minMagnitude the faintest catalog stars to consider
     */
    @Suppress("LongParameterList")
    fun plateSolve(
        readings: List<CelestialObservation>,
        time: Instant,
        approximateLocation: Coordinate? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        tolerance: Float = 0.04f,
        minMatches: Int = 5,
        minMagnitude: Float = 4f
    ): List<DetectedStar> {
        val maxZenithOffset = if (approximateLocation == null) {
            PlateSolver.UNKNOWN_LOCATION_ZENITH_OFFSET
        } else {
            PlateSolver.DEFAULT_MAX_ZENITH_OFFSET
        }
        val solver = PlateSolver(tolerance, minMatches, minMagnitude, maxZenithOffset)
        return solver.solve(readings, time, approximateLocation ?: Time.getLocationFromTimeZone(timeZone))
    }

    /**
     * Gets the distance from the observer to the point directly below a star with the given altitude.
     */
    fun getZenithDistance(altitude: Float): Distance {
        return Distance.nauticalMiles((90f - altitude) * 60)
    }

    fun getLocationFromStars(
        starReadings: List<StarReading>,
        approximateLocation: Coordinate? = null
    ): Coordinate? {
        return StarLocationCalculator().getLocationFromStars(starReadings, approximateLocation)
    }

    /**
     * Identifies the stars in a photo of the sky. Only the angles between stars are used, so no compass is needed.
     */
    fun identifyStars(
        image: SkyImage,
        approximateLocation: Coordinate,
        settings: StarIdentifierSettings = StarIdentifierSettings()
    ): StarIdentification? {
        return CelestialNavigationFacade.identifyStars(image, approximateLocation, settings)
    }

    /**
     * Gets the location from a photo of the sky using gravity's up direction (SkyImage.up)
     * @param deflection converts the astronomic location to geodetic, if provided
     */
    fun getLocationFromStars(
        image: SkyImage,
        approximateLocation: Coordinate,
        settings: StarIdentifierSettings = StarIdentifierSettings(),
        deflection: VerticalDeflectionModel? = null
    ): Coordinate? {
        return CelestialNavigationFacade.getLocationFromStars(image, approximateLocation, settings, deflection)
    }

    /**
     * Gets the camera's true orientation from a photo of the sky at a known location. Gravity is
     * optional, it only narrows the search.
     * @return the identification, with a rotation from the camera's frame to East-North-Up
     */
    fun getOrientationFromStars(
        image: SkyImage,
        location: Coordinate,
        settings: StarIdentifierSettings = StarIdentifierSettings(maxZenithOffsetDegrees = 5.0)
    ): StarIdentification? {
        return CelestialNavigationFacade.getOrientationFromStars(image, location, settings)
    }

    // endregion

    // region Planets

    fun getPlanetPosition(
        planet: Planet,
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return PlanetCalculationModel.position(planet, time, location, withRefraction, withParallax)
    }

    /**
     * Gets the rise, transit, and set times of the planet on the local day containing [time].
     */
    @Suppress("LongParameterList")
    fun getPlanetEvents(
        planet: Planet,
        time: Instant,
        location: Coordinate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {
        return PlanetCalculationModel.events(planet, time, timeZone, location, withRefraction, withParallax)
    }

    // endregion

    /**
     * Gets the first event after [time] on the local day of [time] or the day after.
     */
    private inline fun nextEvent(
        time: Instant,
        timeZone: TimeZone,
        event: (RiseSetTransitTimes) -> Instant?,
        events: (day: Instant) -> RiseSetTransitTimes
    ): Instant? {
        for (day in 0..1) {
            val candidate = event(events(time.plus(day, DateTimeUnit.DAY, timeZone)))
            if (candidate != null && candidate > time) {
                return candidate
            }
        }
        return null
    }

    private inline fun aboveHorizonTimes(
        time: Instant,
        timeZone: TimeZone,
        nextRiseOffset: Duration,
        isUp: Boolean,
        events: (day: Instant) -> RiseSetTransitTimes
    ): Range<Instant>? {
        val nearbyEvents = (-1..1).map { events(time.plus(it, DateTimeUnit.DAY, timeZone)) }
        val rises = nearbyEvents.mapNotNull { it.rise }
        val sets = nearbyEvents.mapNotNull { it.set }
        val lastRise = rises.filter { it < time }.maxOrNull()
        val nextRise = rises.filter { it > time }.minOrNull()
        val lastSet = sets.filter { it < time }.maxOrNull()
        val nextSet = sets.filter { it > time }.minOrNull()

        val date = time.toLocalDateTime(timeZone).date
        val startOfDay = date.atStartOfDayIn(timeZone)
        val endOfDay = date.atEndOfDayIn(timeZone)

        return when {
            // Currently up: from the last rise until the next set
            isUp -> Range(lastRise ?: startOfDay, nextSet ?: endOfDay)
            // Rising soon: from the next rise until the following set
            nextRise != null && nextRise - time <= nextRiseOffset -> Range(nextRise, nextSet ?: endOfDay)
            // Never rose or set nearby
            lastRise == null && lastSet == null -> null
            // Already set: from the last rise until the last set
            else -> Range(lastRise ?: startOfDay, lastSet ?: endOfDay)
        }
    }

    private const val SUPER_MOON_MAX_DISTANCE_KM = 360000f
}
