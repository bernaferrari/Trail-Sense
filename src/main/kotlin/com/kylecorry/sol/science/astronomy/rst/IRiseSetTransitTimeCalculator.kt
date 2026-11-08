package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.JvmRiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.JvmCelestialLocator
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime

internal interface IRiseSetTransitTimeCalculator {
    fun calculate(
        locator: JvmCelestialLocator,
        date: ZonedDateTime,
        location: Coordinate,
        standardAltitude: Double = 0.0,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): JvmRiseSetTransitTimes
}
