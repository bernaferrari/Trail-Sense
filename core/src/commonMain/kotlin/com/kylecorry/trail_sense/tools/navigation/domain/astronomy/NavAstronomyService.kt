package com.kylecorry.trail_sense.tools.navigation.domain.astronomy

import com.kylecorry.trail_sense.shared.time.today

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService

class NavAstronomyService(private val astronomy: AstronomyService = AstronomyService()) {

    fun getData(location: Coordinate): NavAstronomyData {
        val moonPosition = astronomy.getMoonPosition(location)
        val sunPosition = astronomy.getSunPosition(location)
        return NavAstronomyData(
            sunPosition.azimuth.value,
            moonPosition.azimuth.value,
            astronomy.isSunUp(location),
            astronomy.isMoonUp(location),
            astronomy.getMoonPhase(today()).phase,
            astronomy.getMoonTilt(location).parallacticAngle
        )
    }
}
