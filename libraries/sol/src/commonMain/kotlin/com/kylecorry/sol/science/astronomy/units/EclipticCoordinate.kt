package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.tanDegrees
import kotlin.math.asin
import kotlin.math.atan2

internal class EclipticCoordinate(paramEclipticLatitude: Double, paramEclipticLongitude: Double) {
    val eclipticLatitude = wrap(paramEclipticLatitude, -90.0, 90.0)
    val eclipticLongitude = wrap(paramEclipticLongitude, 0.0, 360.0)

    fun toEquatorial(eclipticObliquity: Double): EquatorialCoordinate {
        val rightAscension = atan2(
            sinDegrees(eclipticLongitude) * cosDegrees(eclipticObliquity) - tanDegrees(eclipticLatitude) * sinDegrees(
                eclipticObliquity
            ),
            cosDegrees(eclipticLongitude)
        ).toDegrees()

        val declination = asin(
            sinDegrees(eclipticLatitude) * cosDegrees(eclipticObliquity) + cosDegrees(eclipticLatitude) * sinDegrees(
                eclipticObliquity
            ) * sinDegrees(eclipticLongitude)
        ).toDegrees()

        return EquatorialCoordinate(declination, rightAscension)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EclipticCoordinate

        if (eclipticLatitude != other.eclipticLatitude) return false
        if (eclipticLongitude != other.eclipticLongitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eclipticLatitude.hashCode()
        result = 31 * result + eclipticLongitude.hashCode()
        return result
    }


    companion object {
        fun getObliquityOfTheEcliptic(julianCenturies: Double): Double {
            val e0 = 23.439292
            val t = julianCenturies
            return e0 - Algebra.polynomial(t, 0.0, 46.815, 0.0006, -0.00181) / 3600
        }

        fun fromEquatorial(
            equatorial: EquatorialCoordinate,
            julianCenturies: Double
        ): EclipticCoordinate {
            val e = getObliquityOfTheEcliptic(julianCenturies)
            val alpha = equatorial.rightAscension
            val delta = equatorial.declination
            val t0 = sinDegrees(delta) * cosDegrees(e)
            val t1 = cosDegrees(delta) * sinDegrees(e) * sinDegrees(alpha)

            val t = t0 - t1
            val lat = asin(t).toDegrees()
            val y =
                sinDegrees(alpha) * cosDegrees(e) + tanDegrees(delta) * sinDegrees(
                    e
                )

            val x = cosDegrees(alpha)

            var lon = atan2(y, x).toDegrees()

            if (equatorial.isApparent) {
                val omega = Algebra.polynomial(julianCenturies, 125.04, -1934.136)
                lon += 0.00569 + 0.00478 * sinDegrees(omega)
            }

            return EclipticCoordinate(lat, lon)
        }

    }


}
