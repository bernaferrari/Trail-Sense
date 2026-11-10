package com.kylecorry.sol.science.astronomy.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.ut
import kotlin.time.Duration.Companion.days

internal class EclipticCoordinateTest {

    @Test
    fun toEquatorial() {
        val ut = (ut(2000, 1, 1, 0, 0) - 1.days)
        val ecliptic = EclipticCoordinate(1.2, 184.6)
        val equatorial = ecliptic.toEquatorial(EclipticCoordinate.getObliquityOfTheEcliptic(ut.toJulianCenturies()))

        assertEquals(184.697898, equatorial.rightAscension, 0.00001)
        assertEquals(-0.726531, equatorial.declination, 0.00001)
    }

    @Test
    fun fromEquatorial() {
        val ut = (ut(2000, 1, 1, 0, 0) - 1.days)
        val equatorial = EquatorialCoordinate(-0.726528, 184.697917)
        val ecliptic = EclipticCoordinate.fromEquatorial(equatorial, ut.toJulianCenturies())

        assertEquals(1.20001, ecliptic.eclipticLatitude, 0.00001)
        assertEquals(184.600016, ecliptic.eclipticLongitude, 0.00001)
    }

    @Test
    fun getObliquityOfTheEcliptic() {
        val ut = (ut(2010, 1, 1, 0, 0) - 1.days)
        val e2010 = EclipticCoordinate.getObliquityOfTheEcliptic(ut.toJulianCenturies())
        assertEquals(23.437992, e2010, 0.000001)
    }

}