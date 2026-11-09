package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.astronomy.eclipse.EclipseType
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.stars.CONSTELLATIONS
import com.kylecorry.sol.science.astronomy.stars.Constellation
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.time.atStartOfDay
import com.kylecorry.trail_sense.shared.time.plusDays
import com.kylecorry.trail_sense.shared.time.systemTimeZone
import com.kylecorry.trail_sense.shared.time.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The facade for astronomy related services
 *
 * @param clock the source of the current time
 * @param timeZone the time zone used to interpret dates, defaults to the device time zone
 */
class AstronomyService(
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone? = null
) {

    private val zone: TimeZone
        get() = timeZone ?: systemTimeZone

    private fun now(): Instant = clock.now()

    private fun today(): LocalDate = clock.todayIn(zone)

    private fun LocalDate.atNoon(): Instant = atTime(12, 0).toInstant(zone)

    // PUBLIC MOON METHODS

    fun getCurrentMoonPhase(): MoonPhase {
        return getMoonPhase(now())
    }

    /**
     * Gets the moon phase at noon (should this be rise/set?)
     */
    fun getMoonPhase(date: LocalDate): MoonPhase {
        return getMoonPhase(date.atNoon())
    }

    fun getMoonPhase(time: Instant): MoonPhase {
        return Astronomy.getMoonPhase(time)
    }

    fun isSuperMoon(date: LocalDate): Boolean {
        return Astronomy.isSuperMoon(date.atNoon())
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): RiseSetTransitTimes {
        return Astronomy.getMoonEvents(
            date.atStartOfDay(zone),
            location,
            zone,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: Instant
    ): List<Reading<Float>> {
        return Time.getReadings(
            time - 12.hours,
            time + 12.hours,
            altitudeGranularity
        ) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return Time.getReadings(
            date,
            zone,
            altitudeGranularity
        ) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getMoonPosition(
        location: Coordinate,
        time: Instant = now()
    ): CelestialObservation {
        return Astronomy.getMoonPosition(time, location, withRefraction = true, withParallax = true)
    }

    fun getMoonAltitude(location: Coordinate, time: Instant = now()): Float {
        return getMoonPosition(location, time).altitude
    }

    fun getMoonAzimuth(location: Coordinate, time: Instant = now()): Bearing {
        return getMoonPosition(location, time).azimuth
    }

    fun isMoonUp(location: Coordinate, time: Instant = now()): Boolean {
        return Astronomy.isMoonUp(
            time,
            location,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getMoonAboveHorizonTimes(location: Coordinate, time: Instant): Range<Instant>? {
        return Astronomy.getMoonAboveHorizonTimes(
            location,
            time,
            zone,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getMoonTilt(
        location: Coordinate,
        time: Instant = now(),
        useNearestTransit: Boolean = false
    ): MoonTilt {
        val timeToUse = if (useNearestTransit) {
            val date = time.toLocalDate(zone)
            getMoonTimes(location, date).transit
                ?: getMoonTimes(location, date.plusDays(-1)).transit
                ?: date.atStartOfDay(zone)
        } else {
            time
        }

        val parallacticAngle = Astronomy.getMoonParallacticAngle(timeToUse, location)
        val brightLimbAngleFromNorth = Astronomy.getMoonPositionAngleOfBrightLimb(timeToUse)
        return MoonTilt(parallacticAngle, brightLimbAngleFromNorth)
    }

    // PUBLIC SUN METHODS

    fun getSunTimes(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): RiseSetTransitTimes {
        return Astronomy.getSunEvents(
            date.atStartOfDay(zone),
            location,
            zone,
            sunTimesMode,
            true
        )
    }

    fun getLengthOfDay(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): Duration {
        return Astronomy.getDaylightLength(
            date.atStartOfDay(zone),
            location,
            zone,
            sunTimesMode,
            true
        )
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, today())
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, today().plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return Time.getReadings(
            date,
            zone,
            altitudeGranularity
        ) {
            getSunPosition(location, it).altitude
        }
    }

    fun getCenteredSunAltitudes(
        location: Coordinate,
        time: Instant
    ): List<Reading<Float>> {
        return Time.getReadings(
            time - 12.hours,
            time + 12.hours,
            altitudeGranularity
        ) {
            getSunPosition(location, it).altitude
        }
    }

    fun getNextSunset(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: Instant = now()
    ): Instant? {
        return Astronomy.getNextSunset(time, location, zone, sunTimesMode, true)
    }

    fun getNextSunrise(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: Instant = now()
    ): Instant? {
        return Astronomy.getNextSunrise(time, location, zone, sunTimesMode, true)
    }

    fun isSunUp(location: Coordinate, time: Instant = now()): Boolean {
        return Astronomy.isSunUp(time, location, true)
    }

    fun getSunAzimuth(location: Coordinate, time: Instant = now()): Bearing {
        return getSunPosition(location, time).azimuth
    }

    fun getSunAltitude(location: Coordinate, time: Instant = now()): Float {
        return getSunPosition(location, time).altitude
    }

    fun getSunPosition(
        location: Coordinate,
        time: Instant = now()
    ): CelestialObservation {
        return Astronomy.getSunPosition(time, location, withRefraction = true)
    }

    fun getSunAboveHorizonTimes(location: Coordinate, time: Instant): Range<Instant>? {
        return Astronomy.getSunAboveHorizonTimes(
            location, time, zone,
            withRefraction = true
        )
    }

    fun getMeteorShower(
        location: Coordinate,
        date: LocalDate = today()
    ): MeteorShowerPeak? {
        val noon = date.atNoon()
        return Astronomy.getMeteorShower(location, noon, zone)
            ?: Astronomy.getMeteorShower(location, noon + 1.days, zone)
    }

    fun getMeteorShowerPeakAltitude(peak: MeteorShowerPeak, location: Coordinate): Float {
        return Astronomy.getMeteorShowerPosition(
            peak.shower,
            location,
            peak.peak
        ).altitude
    }

    fun getMeteorShowerPeakAzimuth(peak: MeteorShowerPeak, location: Coordinate): Bearing {
        return Astronomy.getMeteorShowerPosition(
            peak.shower,
            location,
            peak.peak
        ).azimuth
    }

    fun getSeason(location: Coordinate, date: LocalDate = today()): Season {
        return Astronomy.getSeason(location, date.atStartOfDay(zone))
    }

    fun getLunarEclipse(
        location: Coordinate,
        date: LocalDate = today()
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.PartialLunar) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getLunarEclipseShadow(
        location: Coordinate,
        time: Instant = now()
    ): LunarEclipseShadow {
        return Astronomy.getLunarEclipseShadow(
            time,
            location,
            zone,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getSolarEclipse(
        location: Coordinate,
        date: LocalDate = today()
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.Solar, 1) {
            getSunPosition(location, it).altitude
        }
    }

    fun getSolarEclipseObscuration(
        location: Coordinate,
        time: Instant = now()
    ): Float? {
        return Astronomy.getEclipseObscuration(time, location, EclipseType.Solar)
    }

    fun getPeakSolarEclipseObscuration(
        location: Coordinate,
        time: Instant = now()
    ): Float? {
        val hoursToMinutes = 60.0
        val toleranceMinutes = 5.0
        val startTime = 6
        val endTime = 20
        val startOfDay = time.toLocalDate(zone).atStartOfDay(zone)

        val optimizer = GoldenSearchExtremaFinder(hoursToMinutes, toleranceMinutes)
        return optimizer.find(Range(startTime * hoursToMinutes, endTime * hoursToMinutes)) {
            val newTime = startOfDay + it.minutes
            getSolarEclipseObscuration(location, newTime)?.toDouble() ?: 0.0
        }.firstOrNull { it.isHigh }?.point?.y
    }

    fun getVisibleStars(
        location: Coordinate,
        time: Instant = now(),
        thresholdElevation: Float? = 0f,
        maxMagnitude: Float? = 4.0f
    ): List<Pair<Star, Pair<Bearing, Float>>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        return STAR_CATALOG.filter { maxMagnitude == null || it.magnitude <= maxMagnitude }.map {
            val position = getStarPosition(it, location, time)
            val azimuth = position.azimuth
            val altitude = position.altitude
            it to (azimuth to altitude)
        }.filter { thresholdElevation == null || it.second.second > thresholdElevation }
    }

    fun getConstellationsForStar(star: Star): List<Constellation> {
        return CONSTELLATIONS.filter { it.allStarIds.contains(star.hipDesignation) }
    }

    fun getStarPosition(
        star: Star,
        location: Coordinate,
        time: Instant = now()
    ): CelestialObservation {
        return Astronomy.getStarPosition(star, time, location, true)
    }

    fun getVisiblePlanets(
        location: Coordinate,
        time: Instant = now(),
        thresholdElevation: Float? = 0f,
        includeDimPlanets: Boolean = false
    ): List<Pair<Planet, CelestialObservation>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        // Initial filter based on magnitude / proximity to the sun
        val planetsToConsider = buildList {
            add(Planet.Venus)
            add(Planet.Mars)
            add(Planet.Jupiter)
            add(Planet.Saturn)
            if (includeDimPlanets) {
                add(Planet.Mercury)
                add(Planet.Uranus)
                add(Planet.Neptune)
            }
        }

        return planetsToConsider.map {
            it to Astronomy.getPlanetPosition(it, time, location, withRefraction = true)
        }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    fun getVisibleMeteorShowers(
        location: Coordinate,
        time: Instant = now(),
        thresholdElevation: Float? = -10f
    ): List<Pair<MeteorShower, CelestialObservation>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        val showers = Astronomy.getActiveMeteorShowers(location, time, zone)

        return showers
            .filter {
                (it.peak - time).absoluteValue <= (it.shower.activeDays / 3).days
            }
            .map { shower ->
                shower.shower to Astronomy.getMeteorShowerPosition(
                    shower.shower,
                    location,
                    time
                )
            }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    private fun getEclipse(
        location: Coordinate,
        date: LocalDate,
        eclipseType: EclipseType,
        days: Long = 2,
        getPeakAltitude: (Instant) -> Float
    ): Eclipse? {
        val nextEclipse = Astronomy.getNextEclipse(
            date.atStartOfDay(zone),
            location,
            eclipseType,
            days.days
        ) ?: return null

        if (nextEclipse.start.toLocalDate(zone) != date && nextEclipse.end.toLocalDate(zone) != date) {
            return null
        }

        return Eclipse(
            nextEclipse.start,
            nextEclipse.end,
            nextEclipse.maximum,
            nextEclipse.magnitude,
            nextEclipse.obscuration,
            getPeakAltitude(nextEclipse.maximum)
        )
    }

    suspend fun findNextEvent(
        event: AstronomyEvent,
        location: Coordinate,
        start: LocalDate = today(),
        maxSearch: Duration = (365 * 2).days
    ): LocalDate? = withContext(Dispatchers.Default) {
        // An update to this algorithm is planned to make it more efficient
        var isInEvent = hasEvent(event, location, start)
        var date = start.plusDays(1)
        val end = start.plusDays(maxSearch.inWholeDays)
        while (date <= end) {
            ensureActive()
            val isEventDay = hasEvent(event, location, date)
            if (isEventDay && !isInEvent) {
                return@withContext date
            }
            isInEvent = isEventDay
            date = date.plusDays(1)
        }
        return@withContext null
    }

    private fun hasEvent(event: AstronomyEvent, location: Coordinate, date: LocalDate): Boolean {
        return when (event) {
            AstronomyEvent.FullMoon -> getMoonPhase(date).phase == MoonTruePhase.Full
            AstronomyEvent.NewMoon -> getMoonPhase(date).phase == MoonTruePhase.New
            AstronomyEvent.QuarterMoon -> getMoonPhase(date).phase in listOf(
                MoonTruePhase.FirstQuarter,
                MoonTruePhase.ThirdQuarter
            )

            AstronomyEvent.MeteorShower -> getMeteorShower(location, date)?.peak?.toLocalDate(zone) == date
            AstronomyEvent.LunarEclipse -> getLunarEclipse(location, date) != null
            AstronomyEvent.Supermoon -> isSuperMoon(date)
            AstronomyEvent.SolarEclipse -> getSolarEclipse(location, date) != null
        }
    }

    companion object {
        private val altitudeGranularity = 10.minutes
        const val SUN_MIN_ALTITUDE_ACTUAL = -0.8333f
        const val SUN_MIN_ALTITUDE_CIVIL = -6f
        const val SUN_MIN_ALTITUDE_NAUTICAL = -12f
        const val SUN_MIN_ALTITUDE_ASTRONOMICAL = -18f
    }
}
