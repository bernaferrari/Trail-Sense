package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.trail_sense.shared.time.JavaTime.toInstant

import com.kylecorry.trail_sense.shared.time.interop.*

import com.kylecorry.luna.concurrency.onDefault
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
import com.kylecorry.trail_sense.shared.time.JavaTime
import com.kylecorry.trail_sense.shared.time.JavaTime.atStartOfDay
import com.kylecorry.trail_sense.shared.time.JavaTime.toZonedDateTime
import com.kylecorry.trail_sense.shared.time.JavaTime.toKotlinInstant
import com.kylecorry.trail_sense.shared.time.JavaTime.toKotlinTimeZone
import com.kylecorry.trail_sense.shared.time.JavaTime.toZonedDateTimeRange
import com.kylecorry.trail_sense.shared.time.JavaTime.toLocalDateTime
import com.kylecorry.trail_sense.shared.time.JavaTime.toLocalDate
import com.kylecorry.trail_sense.shared.time.JavaTime.toJava
import com.kylecorry.trail_sense.shared.time.JavaTime.toKotlin
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.ensureActive
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The facade for astronomy related services
 */
class AstronomyService(private val clock: Clock = Clock.systemDefaultZone()) {

    // PUBLIC MOON METHODS

    fun getCurrentMoonPhase(): MoonPhase {
        return getMoonPhase(ZonedDateTime.now(clock))
    }

    /**
     * Gets the moon phase at noon (should this be rise/set?)
     */
    fun getMoonPhase(date: LocalDate): MoonPhase {
        val time = date.atTime(12, 0).toZonedDateTime()
        return getMoonPhase(time)
    }

    fun getMoonPhase(time: ZonedDateTime): MoonPhase {
        return Astronomy.getMoonPhase(time.toKotlinInstant())
    }

    fun isSuperMoon(date: LocalDate): Boolean {
        val time = date.atTime(12, 0).toZonedDateTime()
        return Astronomy.isSuperMoon(time.toKotlinInstant())
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): RiseSetTransitTimes {
        val start = date.atStartOfDay().toZonedDateTime()
        return Astronomy.getMoonEvents(
            start.toKotlinInstant(),
            location,
            start.toKotlinTimeZone(),
            withRefraction = true,
            withParallax = true
        )
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.minusHours(12)
        val endTime = time.plusHours(12)
        return JavaTime.getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return JavaTime.getReadings(
            date,
            ZoneId.systemDefault(),
            altitudeGranularity
        ) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getMoonPosition(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): CelestialObservation {
        return Astronomy.getMoonPosition(time.toKotlinInstant(), location, withRefraction = true, withParallax = true)
    }

    fun getMoonAltitude(location: Coordinate, time: kotlin.time.Instant): Float {
        return getMoonAltitude(location, time.toZonedDateTime())
    }

    fun getMoonAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return getMoonPosition(location, time).altitude
    }

    fun getMoonAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return getMoonPosition(location, time).azimuth
    }

    fun isMoonUp(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now(clock)): Boolean {
        return Astronomy.isMoonUp(
            time.toKotlinInstant(),
            location,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getMoonAboveHorizonTimes(location: Coordinate, time: ZonedDateTime): Range<ZonedDateTime>? {
        return Astronomy.getMoonAboveHorizonTimes(
            location,
            time.toKotlinInstant(),
            time.toKotlinTimeZone(),
            withRefraction = true,
            withParallax = true
        )?.toZonedDateTimeRange(time.zone)
    }

    fun getMoonTilt(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        useNearestTransit: Boolean = false
    ): MoonTilt {
        val timeToUse = if (useNearestTransit) {
            getMoonTimes(location, time.toLocalDate()).transit ?: getMoonTimes(
                location,
                time.toLocalDate().minusDays(1)
            ).transit ?: time.toLocalDate().atStartOfDay().toZonedDateTime().toKotlinInstant()
        } else {
            time.toKotlinInstant()
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
        val start = date.atStartOfDay().toZonedDateTime()
        return Astronomy.getSunEvents(
            start.toKotlinInstant(),
            location,
            start.toKotlinTimeZone(),
            sunTimesMode,
            true
        )
    }

    fun getLengthOfDay(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): Duration {
        val start = date.atStartOfDay().toZonedDateTime()
        return Astronomy.getDaylightLength(
            start.toKotlinInstant(),
            location,
            start.toKotlinTimeZone(),
            sunTimesMode,
            true
        ).toJava()
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return JavaTime.getReadings(
            date,
            ZoneId.systemDefault(),
            altitudeGranularity
        ) {
            getSunPosition(location, it).altitude
        }
    }

    fun getCenteredSunAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.minusHours(12)
        val endTime = time.plusHours(12)
        return JavaTime.getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getSunPosition(location, it).altitude
        }
    }

    fun getNextSunset(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: ZonedDateTime = ZonedDateTime.now(clock)
    ): LocalDateTime? {
        return Astronomy.getNextSunset(time.toKotlinInstant(), location, time.toKotlinTimeZone(), sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun getNextSunrise(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: ZonedDateTime = ZonedDateTime.now(clock)
    ): LocalDateTime? {
        return Astronomy.getNextSunrise(time.toKotlinInstant(), location, time.toKotlinTimeZone(), sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun isSunUp(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now(clock)): Boolean {
        return Astronomy.isSunUp(time.toKotlinInstant(), location, true)
    }

    fun getSunAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return getSunPosition(location, time).azimuth
    }

    fun getSunAltitude(location: Coordinate, time: kotlin.time.Instant): Float {
        return getSunAltitude(location, time.toZonedDateTime())
    }

    fun getSunAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return getSunPosition(location, time).altitude
    }

    fun getSunPosition(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): CelestialObservation {
        return Astronomy.getSunPosition(time.toKotlinInstant(), location, withRefraction = true)
    }

    fun getSunAboveHorizonTimes(location: Coordinate, time: ZonedDateTime): Range<ZonedDateTime>? {
        return Astronomy.getSunAboveHorizonTimes(
            location, time.toKotlinInstant(), time.toKotlinTimeZone(),
            withRefraction = true
        )?.toZonedDateTimeRange(time.zone)
    }

    fun getMeteorShower(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): MeteorShowerPeak? {
        val today = date.atTime(12, 0).toZonedDateTime()
        return Astronomy.getMeteorShower(location, today.toKotlinInstant(), today.toKotlinTimeZone())
            ?: Astronomy.getMeteorShower(location, today.plusDays(1).toKotlinInstant(), today.toKotlinTimeZone())
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

    fun getSeason(location: Coordinate, date: LocalDate = LocalDate.now()): Season {
        return Astronomy.getSeason(
            location,
            date.atStartOfDay(ZoneId.systemDefault()).toKotlinInstant()
        )
    }

    fun getLunarEclipse(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.PartialLunar) {
            getMoonPosition(location, it).altitude
        }
    }

    fun getLunarEclipseShadow(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): LunarEclipseShadow {
        return Astronomy.getLunarEclipseShadow(
            time.toKotlinInstant(),
            location,
            time.toKotlinTimeZone(),
            withRefraction = true,
            withParallax = true
        )
    }


    fun getSolarEclipse(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.Solar, 1) {
            getSunPosition(location, it).altitude
        }
    }

    fun getSolarEclipseObscuration(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): Float? {
        return Astronomy.getEclipseObscuration(time.toKotlinInstant(), location, EclipseType.Solar)
    }

    fun getPeakSolarEclipseObscuration(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): Float? {
        val hoursToMinutes = 60.0
        val toleranceMinutes = 5.0
        val startTime = 6
        val endTime = 20

        val optimizer = GoldenSearchExtremaFinder(hoursToMinutes, toleranceMinutes)
        return optimizer.find(Range(startTime * hoursToMinutes, endTime * hoursToMinutes)) {
            val newTime = time.atStartOfDay().plus(JavaTime.hours(it / hoursToMinutes))
            getSolarEclipseObscuration(location, newTime)?.toDouble() ?: 0.0
        }.firstOrNull { it.isHigh }?.point?.y
    }

    fun getVisibleStars(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
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
        time: ZonedDateTime = ZonedDateTime.now()
    ): CelestialObservation {
        return Astronomy.getStarPosition(star, time.toKotlinInstant(), location, true)
    }

    fun getVisiblePlanets(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
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
            it to Astronomy.getPlanetPosition(it, time.toKotlinInstant(), location, withRefraction = true)
        }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    fun getVisibleMeteorShowers(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        thresholdElevation: Float? = -10f
    ): List<Pair<MeteorShower, CelestialObservation>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        val showers = Astronomy.getActiveMeteorShowers(location, time.toKotlinInstant(), time.toKotlinTimeZone())

        return showers
            .filter {
                Duration.between(time.toInstant(), it.peak.toJava())
                    .abs() <= Duration.ofDays(it.shower.activeDays.toLong() / 3)
            }
            .map { shower ->
                shower.shower to Astronomy.getMeteorShowerPosition(
                    shower.shower,
                    location,
                    time.toKotlinInstant()
                )
            }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    private fun getEclipse(
        location: Coordinate,
        date: LocalDate,
        eclipseType: EclipseType,
        days: Long = 2,
        getPeakAltitude: (ZonedDateTime) -> Float
    ): Eclipse? {
        val nextEclipse = Astronomy.getNextEclipse(
            date.atStartOfDay(ZoneId.systemDefault()).toKotlinInstant(),
            location,
            eclipseType,
            Duration.ofDays(days).toKotlin()
        ) ?: return null

        val start = nextEclipse.start.toZonedDateTime()
        val end = nextEclipse.end.toZonedDateTime()
        val peak = nextEclipse.maximum.toZonedDateTime()

        if (start.toLocalDate() != date && end.toLocalDate() != date) {
            return null
        }

        return Eclipse(
            start,
            end,
            peak,
            nextEclipse.magnitude,
            nextEclipse.obscuration,
            getPeakAltitude(peak)
        )
    }

    suspend fun findNextEvent(
        event: AstronomyEvent,
        location: Coordinate,
        start: LocalDate = LocalDate.now(),
        maxSearch: Duration = Duration.ofDays(365 * 2L)
    ): LocalDate? = onDefault {
        // An update to this algorithm is planned to make it more efficient
        var isInEvent = hasEvent(event, location, start)
        var date = start.plusDays(1)
        val end = start.plusDays(maxSearch.toDays())
        while (date <= end) {
            ensureActive()
            val isEventDay = hasEvent(event, location, date)
            if (isEventDay && !isInEvent) {
                return@onDefault date
            }
            isInEvent = isEventDay
            date = date.plusDays(1)
        }
        return@onDefault null
    }

    private fun hasEvent(event: AstronomyEvent, location: Coordinate, date: LocalDate): Boolean {
        return when (event) {
            AstronomyEvent.FullMoon -> getMoonPhase(date).phase == MoonTruePhase.Full
            AstronomyEvent.NewMoon -> getMoonPhase(date).phase == MoonTruePhase.New
            AstronomyEvent.QuarterMoon -> getMoonPhase(date).phase in listOf(
                MoonTruePhase.FirstQuarter,
                MoonTruePhase.ThirdQuarter
            )

            AstronomyEvent.MeteorShower -> getMeteorShower(location, date)?.peak?.toLocalDate() == date
            AstronomyEvent.LunarEclipse -> getLunarEclipse(location, date) != null
            AstronomyEvent.Supermoon -> isSuperMoon(date)
            AstronomyEvent.SolarEclipse -> getSolarEclipse(location, date) != null
        }
    }

    companion object {
        private val altitudeGranularity = Duration.ofMinutes(10)
        const val SUN_MIN_ALTITUDE_ACTUAL = -0.8333f
        const val SUN_MIN_ALTITUDE_CIVIL = -6f
        const val SUN_MIN_ALTITUDE_NAUTICAL = -12f
        const val SUN_MIN_ALTITUDE_ASTRONOMICAL = -18f
    }
}
