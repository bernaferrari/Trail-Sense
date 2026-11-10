package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import com.kylecorry.sol.science.astronomy.corrections.LongitudinalNutation
import com.kylecorry.sol.science.astronomy.corrections.TerrestrialTime
import com.kylecorry.sol.science.astronomy.locators.ICelestialLocator
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.GreenwichSiderealTime
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Suppress("TooManyFunctions")
internal class NewtonsRiseSetModel {
    @Suppress("LongParameterList")
    fun calculate(locator: ICelestialLocator, date: Instant, zone: TimeZone, location: Coordinate,
                  standardAltitude: Double, withRefraction: Boolean, withParallax: Boolean): RiseSetTransitTimes {
        var rise: Instant? = null
        var transit: Instant? = null
        var set: Instant? = null
        for (attempt in 0 until if (withRefraction) 2 else 1) {
            val result = calculateHelper(locator, date, zone, location, standardAltitude,
                if (attempt == 0) withRefraction else false, withParallax)
            rise = rise ?: result.rise
            transit = transit ?: result.transit
            set = set ?: result.set
            if (rise != null && transit != null && set != null) break
        }
        return RiseSetTransitTimes(rise, transit, set)
    }

    @Suppress("LongParameterList")
    private fun calculateHelper(locator: ICelestialLocator, date: Instant, zone: TimeZone, location: Coordinate,
                                altitude: Double, refraction: Boolean, parallax: Boolean): RiseSetTransitTimes {
        val localDate = date.toLocalDateTime(zone).date
        fun compute(time: Instant) =
            getTransitTimesHelper(time, zone, location, altitude, refraction, parallax, locator)
        val today = compute(date)
        fun isSameDay(time: Instant?) = time?.toLocalDateTime(zone)?.date == localDate
        if (isSameDay(today.rise) && isSameDay(today.transit) && isSameDay(today.set)) return today
        val noon = LocalDateTime(localDate.year, localDate.month, localDate.day, 12, 0).toInstant(zone)
        val others = listOf(today, compute(noon), compute(date.plus(-1, DateTimeUnit.DAY, zone)),
            compute(date.plus(1, DateTimeUnit.DAY, zone)))
        return RiseSetTransitTimes(others.mapNotNull { it.rise }.firstOrNull(::isSameDay),
            others.mapNotNull { it.transit }.firstOrNull(::isSameDay),
            others.mapNotNull { it.set }.firstOrNull(::isSameDay))
    }

    private fun utcMidnightOnDate(date: Instant, zone: TimeZone): Instant {
        val localDate = date.toLocalDateTime(zone).date
        fun utcMidnight(time: Instant) = time.toLocalDateTime(TimeZone.UTC).date.atStartOfDayIn(TimeZone.UTC)
        for (offset in -1..1) {
            val candidate = utcMidnight(date.plus(offset, DateTimeUnit.DAY, zone))
            if (candidate.toLocalDateTime(zone).date == localDate) return candidate
        }
        return utcMidnight(date)
    }

    @Suppress("LongParameterList")
    private fun getTransitTimesHelper(date: Instant, zone: TimeZone, coordinate: Coordinate, altitude: Double,
                                      refraction: Boolean, parallax: Boolean,
                                      locator: ICelestialLocator): RiseSetTransitTimes {
        val ut = utcMidnightOnDate(date, zone)
        val yesterday = utcMidnightOnDate(date.plus(-1, DateTimeUnit.DAY, zone), zone)
        val tomorrow = utcMidnightOnDate(date.plus(1, DateTimeUnit.DAY, zone), zone)
        val coordinates = Triple(
            locator.getCoordinates(yesterday), locator.getCoordinates(ut), locator.getCoordinates(tomorrow)
        )
        val distances = if (parallax) {
            listOf(locator.getDistance(yesterday), locator.getDistance(ut), locator.getDistance(tomorrow))
        } else emptyList()
        val distanceTriple = if (distances.size == 3 && distances.all { it != null })
            Triple(distances[0]!!, distances[1]!!, distances[2]!!) else null
        val times = getRiseSetTransitTimes(ut, coordinate, altitude, refraction, coordinates, distanceTriple)
            ?: return RiseSetTransitTimes(null, null, null)
        fun addHours(hours: Double) = ut + (hours * 3600000).toLong().milliseconds
        return RiseSetTransitTimes(addHours(times.first), addHours(times.second), addHours(times.third))
    }

    private fun siderealToInstant(sidereal: GreenwichSiderealTime, date: LocalDate): Instant {
        val time = date.atStartOfDayIn(TimeZone.UTC)
        val jd = JulianDay.of(time.toLocalDateTime(TimeZone.UTC))
        val year = (date.year - 1).toDouble()
        val century = floor(year / 100)
        val jd0 = floor(365.25 * (year + 4716)) + 2 - century + floor(century / 4) - 1094.5
        val t = (jd0 - 2415020) / 36525.0
        val r = Algebra.polynomial(t, 6.6460656, 2400.051262, 0.00002581)
        val b = 24 - r + 24 * (date.year - 1900)
        var t0 = 0.0657098 * (jd - jd0) - b
        if (t0 < 0) t0 += 24 else if (t0 > 24) t0 -= 24
        var a = sidereal.hours - t0
        if (a < 0) a += 24
        return time + (0.99727 * a * 3600000).toLong().milliseconds
    }

    private fun getApparentSiderealTime(ut: Instant): Double {
        val longitudeNutation = LongitudinalNutation.getNutationInLongitude(JulianDay.centuries(ut))
        val eclipticObliquity = EclipticObliquity.getTrueObliquityOfEcliptic(JulianDay.centuries(ut))
        val meanSidereal = getMeanSiderealTime(ut)
        return meanSidereal + (longitudeNutation * cosDegrees(eclipticObliquity)) / 15.0
    }

    private fun getMeanSiderealTime(ut: Instant): Double {
        val julianCenturiesSinceJ2000 = JulianDay.centuries(ut)
        val theta0 =
            280.46061837 + 360.98564736629 * (JulianDay.of(ut.toLocalDateTime(TimeZone.UTC)) - 2451545.0) +
                0.000387933 * Arithmetic.square(
                julianCenturiesSinceJ2000
            ) - Arithmetic.cube(
                julianCenturiesSinceJ2000
            ) / 38710000.0
        return wrap(theta0, 0.0, 360.0)
    }

    private fun getRiseSetTransitTimes(
        ut: Instant,
        location: Coordinate,
        standardAltitude: Double,
        withRefraction: Boolean,
        coordinates: Triple<EquatorialCoordinate, EquatorialCoordinate, EquatorialCoordinate>,
        distances: Triple<Distance, Distance, Distance>?
    ): Triple<Double, Double, Double>? {
        val apparentSidereal = getApparentSiderealTime(ut)
        val deltaT = TerrestrialTime.getDeltaT(ut.toLocalDateTime(TimeZone.UTC).year)
        val hourAngle = getHourAngle(standardAltitude, location, coordinates.second) ?: return null
        check(hourAngle in 0.0..180.0) { "Hour angle is invalid" }

        var transitDayFraction = wrap(
            (coordinates.second.rightAscension - location.longitude - apparentSidereal) / 360.0,
            0.0,
            1.0
        )
        var riseDayFraction = wrap(transitDayFraction - hourAngle / 360, 0.0, 1.0)
        var setDayFraction = wrap(transitDayFraction + hourAngle / 360, 0.0, 1.0)
        val date = ut.toLocalDateTime(TimeZone.UTC).date

        for (ignored in 0..<MAX_ITERATIONS) {
            val transitDayFractionAdjustment = getTransitAdjustment(
                apparentSidereal,
                transitDayFraction,
                deltaT,
                coordinates,
                location
            )

            val riseDayFractionAdjustment = getRiseSetAdjustment(
                apparentSidereal,
                riseDayFraction,
                deltaT,
                coordinates,
                distances,
                date,
                location,
                withRefraction,
                standardAltitude,
            )
            val setDayFractionAdjustment = getRiseSetAdjustment(
                apparentSidereal,
                setDayFraction,
                deltaT,
                coordinates,
                distances,
                date,
                location,
                withRefraction,
                standardAltitude,
            )

            transitDayFraction = wrap(transitDayFraction + transitDayFractionAdjustment, 0.0, 1.0)
            riseDayFraction = wrap(riseDayFraction + riseDayFractionAdjustment, 0.0, 1.0)
            setDayFraction = wrap(setDayFraction + setDayFractionAdjustment, 0.0, 1.0)

            if (
                abs(transitDayFractionAdjustment) < DONE_THRESHOLD &&
                abs(riseDayFractionAdjustment) < DONE_THRESHOLD &&
                abs(setDayFractionAdjustment) < DONE_THRESHOLD
            ) {
                break
            }
        }

        val riseHour = riseDayFraction * 24
        val transitHour = transitDayFraction * 24
        val setHour = setDayFraction * 24

        return Triple(riseHour, transitHour, setHour)
    }

    private fun getGreenwichSiderealTime(
        apparentSiderealDegrees: Double,
        dayFraction: Double
    ): GreenwichSiderealTime {
        require(dayFraction in 0.0..1.0) { "Day fraction must be between 0 and 1" }
        val siderealDegreesAtTime =
            Trigonometry.normalizeAngle(apparentSiderealDegrees + 360.985647 * dayFraction)
        val siderealHoursAtTime = siderealDegreesAtTime / 15
        return GreenwichSiderealTime(siderealHoursAtTime)
    }

    private fun getHourAngle(
        standardAltitudeDegrees: Double,
        location: Coordinate,
        coordinate: EquatorialCoordinate
    ): Double? {
        val cosineHourAngle =
            (sinDegrees(standardAltitudeDegrees) - sinDegrees(location.latitude) * sinDegrees(
                coordinate.declination
            )) / (cosDegrees(location.latitude) * cosDegrees(coordinate.declination))

        if (cosineHourAngle >= 1 || cosineHourAngle <= -1) return null

        return wrap(acos(cosineHourAngle).toDegrees(), 0.0, 180.0)
    }

    private fun getTerrestrialDayFraction(
        universalDayFraction: Double,
        deltaTSeconds: Double
    ): Double {
        return universalDayFraction + deltaTSeconds / 86400
    }

    private fun getTransitAdjustment(
        apparentSiderealDegrees: Double,
        dayFraction: Double,
        deltaTSeconds: Double,
        coordinates: Triple<EquatorialCoordinate, EquatorialCoordinate, EquatorialCoordinate>,
        location: Coordinate
    ): Double {
        val siderealTime = getGreenwichSiderealTime(apparentSiderealDegrees, dayFraction)
        val terrestrialDayFraction = getTerrestrialDayFraction(dayFraction, deltaTSeconds)
        val interpolatedCoordinate = interpolateCoordinate(
            terrestrialDayFraction,
            coordinates.first,
            coordinates.second,
            coordinates.third
        )
        val hourAngleDegrees =
            interpolatedCoordinate.getHourAngle(siderealTime.atLongitude(location.longitude)) * 15
        return -hourAngleDegrees / 360
    }

    private fun getRiseSetAdjustment(
        apparentSiderealDegrees: Double,
        dayFraction: Double,
        deltaTSeconds: Double,
        coordinates: Triple<EquatorialCoordinate, EquatorialCoordinate, EquatorialCoordinate>,
        distances: Triple<Distance, Distance, Distance>?,
        date: LocalDate,
        location: Coordinate,
        withRefraction: Boolean,
        standardAltitudeDegrees: Double,
    ): Double {
        val siderealTime = getGreenwichSiderealTime(apparentSiderealDegrees, dayFraction)
        val terrestrialDayFraction = getTerrestrialDayFraction(dayFraction, deltaTSeconds)
        val interpolatedCoordinate = interpolateCoordinate(
            terrestrialDayFraction,
            coordinates.first,
            coordinates.second,
            coordinates.third
        )
        val interpolatedDistance =
            distances?.let {
                interpolateDistance(terrestrialDayFraction, it.first, it.second, it.third)
            }
        val hourAngleDegrees =
            interpolatedCoordinate.getHourAngle(siderealTime.atLongitude(location.longitude)) * 15
        val altitudeDegrees =
            AstroUtils.getAltitude(
                interpolatedCoordinate,
                siderealToInstant(siderealTime, date),
                location,
                withRefraction,
                interpolatedDistance
            )
        return getRiseSetAltitudeAdjustment(
            altitudeDegrees,
            standardAltitudeDegrees,
            interpolatedCoordinate.declination,
            location.latitude,
            hourAngleDegrees
        )
    }

    private fun getRiseSetAltitudeAdjustment(
        altitudeDegrees: Float,
        standardAltitudeDegrees: Double,
        declinationDegrees: Double,
        latitudeDegrees: Double,
        hourAngleDegrees: Double
    ): Double {
        val denominator =
            360 * cosDegrees(declinationDegrees) * cosDegrees(latitudeDegrees) * sinDegrees(
                hourAngleDegrees
            )
        return (altitudeDegrees - standardAltitudeDegrees) / denominator
    }


    private fun interpolateCoordinate(
        value: Double,
        first: EquatorialCoordinate,
        second: EquatorialCoordinate,
        third: EquatorialCoordinate
    ): EquatorialCoordinate {
        val normalizedRas = normalizeRightAscensions(
            Triple(
                first.rightAscension,
                second.rightAscension,
                third.rightAscension
            )
        )

        val ra = Interpolation.catmullRom(
            value,
            normalizedRas.first,
            normalizedRas.second,
            normalizedRas.third
        )

        val declination =
            Interpolation.catmullRom(value, first.declination, second.declination, third.declination)

        return EquatorialCoordinate(declination, ra)
    }

    private fun interpolateDistance(
        value: Double,
        first: Distance,
        second: Distance,
        third: Distance
    ): Distance {
        val distance = Interpolation.catmullRom(
            value,
            first.value.toDouble(),
            second.value.toDouble(),
            third.value.toDouble()
        )
        return Distance.from(distance.toFloat(), first.units)
    }

    private fun normalizeRightAscensions(
        rightAscensions: Triple<Double, Double, Double>
    ): Triple<Double, Double, Double> {
        val ra1 = rightAscensions.first
        val ra2 = if (rightAscensions.second < ra1) {
            rightAscensions.second + 360
        } else {
            rightAscensions.second
        }
        val ra3 = if (rightAscensions.third < ra2) {
            rightAscensions.third + 360
        } else {
            rightAscensions.third
        }
        return Triple(ra1, ra2, ra3)
    }

    companion object {
        private const val MAX_ITERATIONS = 20
        private const val DONE_THRESHOLD = 0.0001
    }

}
