package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.OrbitalMath
import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import com.kylecorry.sol.science.astronomy.corrections.TerrestrialTime
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import com.kylecorry.sol.units.Distance
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow

internal class SunModel {

    private val semiMajorAxisLen0 = 1.495985e8

    fun getCoordinates(ut: Instant): EquatorialCoordinate {
        val delta = TerrestrialTime.getDeltaT(ut.toLocalDateTime(TimeZone.UTC).year)
        val tt = ut + (delta * 1000).toLong().milliseconds
        val julianCenturiesSinceJ2000 = JulianDay.centuries(tt)
        val apparentLongitude = getApparentLongitude(julianCenturiesSinceJ2000)
        val correctedObliquity = getObliquityCorrection(julianCenturiesSinceJ2000)
        val rightAscension = Trigonometry.normalizeAngle(
            atan2(
                cosDegrees(correctedObliquity) * sinDegrees(apparentLongitude),
                cosDegrees(apparentLongitude)
            ).toDegrees()
        )
        val declination = wrap(
            asin(sinDegrees(correctedObliquity) * sinDegrees(apparentLongitude)).toDegrees(),
            -90.0, 90.0
        )
        return EquatorialCoordinate(declination, rightAscension, true)
    }

    fun getDistance(ut: Instant): Distance {
        val julianCenturiesSinceJ2000 = JulianDay.centuries(ut)
        val trueAnomaly = getTrueAnomaly(julianCenturiesSinceJ2000)
        val ecc = Algebra.polynomial(julianCenturiesSinceJ2000, 0.01675104, -0.0000418, -0.000000126)
        val f = (1 + ecc * cosDegrees(trueAnomaly)) / (1 - ecc.pow(2))
        return Distance.kilometers((semiMajorAxisLen0 / f).toFloat())
    }

    fun getAngularDiameter(ut: Instant, distance: Distance? = null): Double {
        val au = (distance ?: getDistance(ut)).meters().value / 149597870700.0
        return 2 * 959.63 / (3600 * au)
    }

    fun getMeanAnomaly(ut: Instant): Double {
        val julianCenturiesSinceJ2000 = JulianDay.centuries(ut)
        return getMeanAnomaly(julianCenturiesSinceJ2000)
    }

    private fun getMeanAnomaly(julianCenturiesSinceJ2000: Double): Double {
        return Trigonometry.normalizeAngle(
            Algebra.polynomial(
                julianCenturiesSinceJ2000,
                357.5291092,
                35999.0502909,
                -0.0001536,
                1 / 24490000.0
            )
        )
    }

    private fun getTrueAnomaly(julianCenturiesSinceJ2000: Double): Double {
        val mean = getMeanAnomaly(julianCenturiesSinceJ2000)
        val center = equationOfCenter(julianCenturiesSinceJ2000)
        return OrbitalMath.getTrueAnomaly(mean, center)
    }

    private fun getApparentLongitude(julianCenturiesSinceJ2000: Double): Double {
        val trueLng = getTrueLongitude(julianCenturiesSinceJ2000)
        val omega = Algebra.polynomial(julianCenturiesSinceJ2000, 125.04, -1934.136)
        return trueLng - 0.00569 - 0.00478 * sinDegrees(omega)
    }

    private fun getTrueLongitude(julianCenturiesSinceJ2000: Double): Double {
        val geometricLongitude = getGeometricLongitude(julianCenturiesSinceJ2000)
        val center = equationOfCenter(julianCenturiesSinceJ2000)
        return geometricLongitude + center
    }

    private fun getGeometricLongitude(julianCenturiesSinceJ2000: Double): Double {
        return Trigonometry.normalizeAngle(
            Algebra.polynomial(julianCenturiesSinceJ2000, 280.46646, 36000.76983, 0.0003032)
        )
    }

    private fun equationOfCenter(julianCenturiesSinceJ2000: Double): Double {
        val meanAnomaly = getMeanAnomaly(julianCenturiesSinceJ2000)
        return Algebra.polynomial(julianCenturiesSinceJ2000, 1.914602, -0.004817, -0.000014) * sinDegrees(meanAnomaly) +
                Algebra.polynomial(julianCenturiesSinceJ2000, 0.019993, -0.000101) * sinDegrees(2 * meanAnomaly) +
                0.000289 * sinDegrees(3 * meanAnomaly)
    }

    private fun getObliquityCorrection(julianCenturiesSinceJ2000: Double): Double {
        val e = EclipticObliquity.getMeanObliquityOfEcliptic(julianCenturiesSinceJ2000)
        val omega = Algebra.polynomial(julianCenturiesSinceJ2000, 125.04, -1934.136)
        return e + 0.00256 * cosDegrees(omega)
    }

}
