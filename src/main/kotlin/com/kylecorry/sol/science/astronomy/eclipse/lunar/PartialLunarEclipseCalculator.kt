package com.kylecorry.sol.science.astronomy.eclipse.lunar

import com.kylecorry.sol.science.astronomy.eclipse.Eclipse
import com.kylecorry.sol.science.astronomy.eclipse.EclipseCalculator
import com.kylecorry.sol.units.Coordinate
import java.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal class PartialLunarEclipseCalculator : EclipseCalculator {
    override fun getNextEclipse(after: Instant, location: Coordinate): Eclipse? {
        val result = LunarEclipseModel(false).next(after.toKotlinInstant(), location, TimeZone.currentSystemDefault())
            ?: return null
        return Eclipse(result.start.toJavaInstant(), result.end.toJavaInstant(), result.magnitude,
            result.obscuration, result.maximum.toJavaInstant())
    }
    override fun getMagnitude(time: Instant, location: Coordinate): Float? = null
    override fun getObscuration(time: Instant, location: Coordinate): Float? = null
}
