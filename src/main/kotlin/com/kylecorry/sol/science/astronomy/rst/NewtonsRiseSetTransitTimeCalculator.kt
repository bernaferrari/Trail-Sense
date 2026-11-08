package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.JvmAstroUtils
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.JvmCelestialLocator
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal class NewtonsRiseSetTransitTimeCalculator : IRiseSetTransitTimeCalculator {
    override fun calculate(locator: JvmCelestialLocator, date: ZonedDateTime, location: Coordinate,
                           standardAltitude: Double, withRefraction: Boolean,
                           withParallax: Boolean): RiseSetTransitTimes {
        val times = NewtonsRiseSetModel().calculate(JvmAstroUtils.commonLocator(locator),
            date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone(), location,
            standardAltitude, withRefraction, withParallax)
        return RiseSetTransitTimes(times.rise?.toJavaInstant()?.atZone(date.zone),
            times.transit?.toJavaInstant()?.atZone(date.zone), times.set?.toJavaInstant()?.atZone(date.zone))
    }
}
