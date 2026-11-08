package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.JvmAstroUtils
import com.kylecorry.sol.science.astronomy.JvmRiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.locators.JvmCelestialLocator
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal class RobustRiseSetTransitTimeCalculator : IRiseSetTransitTimeCalculator {
    override fun calculate(
        locator: JvmCelestialLocator, date: ZonedDateTime, location: Coordinate,
        standardAltitude: Double, withRefraction: Boolean, withParallax: Boolean
    ): JvmRiseSetTransitTimes {
        val times = RobustRiseSetModel().calculate(
            JvmAstroUtils.commonLocator(locator), date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone(),
            location, standardAltitude, withRefraction, withParallax
        )
        return JvmRiseSetTransitTimes(
            times.rise?.toJavaInstant()?.atZone(date.zone),
            times.transit?.toJavaInstant()?.atZone(date.zone),
            times.set?.toJavaInstant()?.atZone(date.zone)
        )
    }
}
