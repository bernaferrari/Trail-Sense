package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.JvmAstroUtils
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.JvmCelestialLocator
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant

internal class SearchRiseSetTransitTimeCalculator : IRiseSetTransitTimeCalculator {
    override fun calculate(
        locator: JvmCelestialLocator,
        date: ZonedDateTime,
        location: Coordinate,
        standardAltitude: Double,
        withRefraction: Boolean,
        withParallax: Boolean
    ): RiseSetTransitTimes {
        val times = SearchRiseSetModel().calculate(
            JvmAstroUtils.commonLocator(locator), date.toLocalDate().toKotlinLocalDate(),
            date.zone.toKotlinTimeZone(), location, standardAltitude, withRefraction, withParallax
        )
        return RiseSetTransitTimes(
            times.rise?.toJavaInstant()?.atZone(date.zone),
            times.transit?.toJavaInstant()?.atZone(date.zone),
            times.set?.toJavaInstant()?.atZone(date.zone)
        )
    }
}
