package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.ICelestialLocator
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal class RobustRiseSetModel {
    @Suppress("LongParameterList")
    fun calculate(
        locator: ICelestialLocator, time: Instant, zone: TimeZone, location: Coordinate,
        standardAltitude: Double, withRefraction: Boolean, withParallax: Boolean
    ): RiseSetTransitTimes {
        val newtons = NewtonsRiseSetModel().calculate(
            locator, time, zone, location, standardAltitude, withRefraction, withParallax
        )
        if (newtons.rise != null && newtons.transit != null && newtons.set != null) return newtons
        val search = SearchRiseSetModel().calculate(
            locator, time.toLocalDateTime(zone).date, zone, location, standardAltitude, withRefraction, withParallax
        )
        return RiseSetTransitTimes(
            newtons.rise ?: search.rise, newtons.transit ?: search.transit, newtons.set ?: search.set
        )
    }
}
