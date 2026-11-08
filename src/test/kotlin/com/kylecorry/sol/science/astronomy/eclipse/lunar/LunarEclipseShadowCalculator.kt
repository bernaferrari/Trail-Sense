package com.kylecorry.sol.science.astronomy.eclipse.lunar

import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toKotlinInstant

internal class LunarEclipseShadowCalculator {
    fun getPeakShadow(
        time: ZonedDateTime, location: Coordinate, withRefraction: Boolean = false, withParallax: Boolean = false
    ): LunarEclipseShadow = LunarEclipseShadowModel().getPeakShadow(
        time.toInstant().toKotlinInstant(), time.zone.toKotlinTimeZone(), location, withRefraction, withParallax
    )
}
