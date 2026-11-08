package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateConversionsCommonTest {
    @Test
    fun convertsEclipticAndHorizonCoordinates() {
        val centuries = -1.5 / 36525.0
        val ecliptic = EclipticCoordinate(1.2, 184.6)
        val equatorial = ecliptic.toEquatorial(EclipticCoordinate.getObliquityOfTheEcliptic(centuries))
        assertEquals(184.697898, equatorial.rightAscension, 0.00001)
        assertEquals(-0.726531, equatorial.declination, 0.00001)
        val sidereal = GreenwichSiderealTime(0.0)
        val horizon = HorizonCoordinate.fromEquatorial(
            EquatorialCoordinate.fromHourAngle(-0.508333, 16.495833, sidereal), sidereal, 25.0
        )
        assertEquals(80.525393, horizon.azimuth, 0.00001)
        assertEquals(-20.577738, horizon.altitude, 0.00001)
        assertEquals(84381.448 / 3600.0, EclipticObliquity.getMeanObliquityOfEcliptic(0.0), 1e-9)
    }
}
