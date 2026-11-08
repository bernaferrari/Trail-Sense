package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonPhaseTimeCalculator
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.science.astronomy.units.fromJulianDay
import com.kylecorry.sol.science.astronomy.units.toJulianCenturies
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import java.time.Duration
import kotlin.time.toKotlinInstant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal class Moon : ICelestialLocator {

    private val sun = Sun()
    private val position = MoonPositionModel()
    private fun UniversalTime.asInstant() = toInstant(java.time.ZoneOffset.UTC).toKotlinInstant()

    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate = position.getCoordinates(ut.asInstant())





    override fun getDistance(ut: UniversalTime): Distance = position.getDistance(ut.asInstant())

    fun getAngularDiameter(
        ut: UniversalTime,
        location: Coordinate = Coordinate.zero,
        distance: Distance? = null
    ): Double {
        val distanceKm = (distance ?: getDistance(ut)).convertTo(DistanceUnits.Kilometers).value
        val s = 358743400 / distanceKm
        val sinPi = 6378.14 / distanceKm
        val h = AstroUtils.getAltitude(this, ut, location, true).toDouble()
        return (s * (1 + sinDegrees(h) * sinPi)) * 0.000277778 * 2
    }

    fun getMeanAnomaly(ut: UniversalTime): Double = position.getMeanAnomaly(ut.asInstant())

    fun getPhase(ut: UniversalTime): MoonPhase {
        val illuminationAngle = getMoonIlluminationAngle(ut)
        val illumination = getMoonIllumination(illuminationAngle).toFloat()
        val phaseAngle = Trigonometry.normalizeAngle(360 - illuminationAngle)
        val previousNewMoon = getPreviousNewMoon(ut)
        val age = Duration.between(previousNewMoon, ut).toSeconds() / SECONDS_PER_DAY

        for (phase in MoonTruePhase.entries) {
            if (phase.startAngle <= illuminationAngle && phase.endAngle >= illuminationAngle) {
                return MoonPhase(phase, illumination, phaseAngle.toFloat(), age)
            }

            // Handle new moon
            if (phase.startAngle >= phase.endAngle) {
                if (phase.startAngle <= illuminationAngle || phase.endAngle >= illuminationAngle) {
                    return MoonPhase(phase, illumination, phaseAngle.toFloat(), age)
                }
            }
        }

        return MoonPhase(MoonTruePhase.New, illumination, phaseAngle.toFloat(), age)
    }

    private fun getPreviousNewMoon(ut: UniversalTime): UniversalTime {
        var k = getNextPhaseK(ut, MoonTruePhase.New)
        var newMoon = MoonPhaseTimeCalculator.getNewMoon(k)
        val maxIterations = 10
        var iterationCount = 0
        while (newMoon > ut && iterationCount < maxIterations) {
            iterationCount++
            k--
            newMoon = MoonPhaseTimeCalculator.getNewMoon(k)
        }
        return newMoon
    }

    fun getNextMeanPhase(date: UniversalTime, moonTruePhase: MoonTruePhase): UniversalTime {
        val k = getNextPhaseK(date, moonTruePhase)
        return fromJulianDay(MoonPhaseTimeCalculator.getMeanPhaseJulianEphemerisDay(k))
    }

    fun getNextPhaseK(date: UniversalTime, moonTruePhase: MoonTruePhase): Double {
        val year = date.year
        val day = date.dayOfYear / 365.25
        val hour = (date.hour / 24.0) / 365.25
        val minute = (((date.minute / 60.0) / 24.0) / 365.25)
        val y = year + day + hour + minute
        val k = (y - 2000) * 12.3685

        val ending = when (moonTruePhase) {
            MoonTruePhase.New -> 0.0
            MoonTruePhase.WaningCrescent -> 0.125
            MoonTruePhase.ThirdQuarter -> 0.25
            MoonTruePhase.WaningGibbous -> 0.375
            MoonTruePhase.Full -> 0.5
            MoonTruePhase.WaxingGibbous -> 0.625
            MoonTruePhase.FirstQuarter -> 0.75
            MoonTruePhase.WaxingCrescent -> 0.875
        }

        val intK = floor(k)
        val remainder = k % 1

        return if (remainder > ending) {
            intK + 1.0 + ending
        } else {
            intK + ending
        }
    }

    fun getZenithAngleOfBrightLimb(date: UniversalTime, location: Coordinate): Float {
        val parallacticAngle = AstroUtils.getParallacticAngle(this, date, location)
        val moonPositionAngle = getPositionAngleOfBrightLimb(date)
        return (moonPositionAngle - parallacticAngle).toFloat()
    }

    fun getPositionAngleOfBrightLimb(ut: UniversalTime): Double {
        val sunCoords = sun.getCoordinates(ut)
        val moonCoords = getCoordinates(ut)
        val sunRA = sunCoords.rightAscension.toRadians()
        val sunDec = sunCoords.declination.toRadians()
        val moonRA = moonCoords.rightAscension.toRadians()
        val moonDec = moonCoords.declination.toRadians()

        return atan2(
            cos(sunDec) * sin(sunRA - moonRA),
            sin(sunDec) * cos(moonDec) - cos(sunDec) * sin(moonDec) * cos(sunRA - moonRA)
        ).toDegrees()
    }

    fun getEccentricity(julianCenturiesSinceJ2000: Double): Double = position.getEccentricity(julianCenturiesSinceJ2000)

    private fun getMoonIlluminationAngle(ut: UniversalTime): Double {
        val meanElongation = getMeanElongation(ut)
        val sunMeanAnomaly = sun.getMeanAnomaly(ut)
        val moonMeanAnomaly = getMeanAnomaly(ut)

        val phaseAngle =
            180 - meanElongation - 6.289 * sinDegrees(moonMeanAnomaly) + 2.100 * sinDegrees(
                sunMeanAnomaly
            ) - 1.274 * sinDegrees(2 * meanElongation - moonMeanAnomaly) - 0.658 * sinDegrees(
                2 * meanElongation
            ) - 0.214 * sinDegrees(
                2 * moonMeanAnomaly
            ) - 0.110 * sinDegrees(meanElongation)

        return (phaseAngle + 180) % 360.0
    }

    private fun getMoonIllumination(illuminationAngle: Double): Double {
        return ((1 + cosDegrees(illuminationAngle - 180)) / 2) * 100
    }

    private fun getMeanElongation(ut: UniversalTime): Double = position.getMeanElongation(ut.asInstant())







    companion object {
        const val SYNODIC_MONTH_DAYS = MoonPhaseTimeCalculator.SYNODIC_MONTH_DAYS
        private const val SECONDS_PER_DAY = 86400f
        const val DIAMETER_IN_EARTH_RADII = 0.545
        const val RADIUS_IN_EARTH_RADII = DIAMETER_IN_EARTH_RADII / 2
    }

}
