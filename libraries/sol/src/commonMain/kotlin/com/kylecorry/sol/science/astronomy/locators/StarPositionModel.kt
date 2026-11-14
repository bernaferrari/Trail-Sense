package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import kotlin.time.Instant
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.units.Distance
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot

internal class StarPositionModel(private val star: Star) : ICelestialLocator {
    override fun getCoordinates(ut: Instant): EquatorialCoordinate {
        val t = JulianDay.centuries(ut)

        val coords = star.coordinate
        val motion = star.motion

        val value1 = Algebra.polynomial(
            t,
            0.0,
            2306.2181,
            0.30188,
            0.017998
        ) / 3600

        val value2 = Algebra.polynomial(
            t,
            0.0,
            2306.2181,
            1.09468,
            0.018203
        ) / 3600

        val value3 = Algebra.polynomial(
            t,
            0.0,
            2004.3109,
            -0.42665,
            -0.041833
        ) / 3600

        val processionRA =
            motion.rightAscension * t * 100
        val processionDec = motion.declination * t * 100

        val declination = coords.declination + processionDec
        val rightAscension = coords.rightAscension + processionRA

        val a = cosDegrees(declination) * sinDegrees(rightAscension + value1)
        val cosDecCosRA = cosDegrees(declination) * cosDegrees(rightAscension + value1)
        val b = cosDegrees(value3) * cosDecCosRA - sinDegrees(value3) * sinDegrees(declination)
        val c = sinDegrees(value3) * cosDecCosRA + cosDegrees(value3) * sinDegrees(declination)

        val ra = atan2(a, b).toDegrees() + value2
        val dec = if (declination.absoluteValue > 89.0) {
            acos(hypot(a, b))
        } else {
            asin(c)
        }.toDegrees()

        return EquatorialCoordinate(dec, ra)
    }

    override fun getDistance(ut: Instant): Distance? {
        return null
    }
}
