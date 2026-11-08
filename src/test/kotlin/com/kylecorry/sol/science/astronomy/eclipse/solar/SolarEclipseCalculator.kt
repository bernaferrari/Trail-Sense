package com.kylecorry.sol.science.astronomy.eclipse.solar

import com.kylecorry.sol.science.astronomy.eclipse.JvmEclipse
import com.kylecorry.sol.science.astronomy.eclipse.EclipseCalculator
import com.kylecorry.sol.units.Coordinate
import java.time.Duration
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

internal class SolarEclipseCalculator(
    precision: Duration = Duration.ofMinutes(1), maxDuration: Duration? = null
) : EclipseCalculator {
    private val model = SolarEclipseModel(precision.toKotlinDuration(), maxDuration?.toKotlinDuration())
    override fun getNextEclipse(after: Instant, location: Coordinate): JvmEclipse? {
        val result = model.getNextEclipse(after.toKotlinInstant(), location) ?: return null
        return JvmEclipse(result.start.toJavaInstant(), result.end.toJavaInstant(), result.magnitude,
            result.obscuration, result.maximum.toJavaInstant())
    }
    override fun getMagnitude(time: Instant, location: Coordinate): Float =
        model.getMagnitude(time.toKotlinInstant(), location)
    override fun getObscuration(time: Instant, location: Coordinate): Float =
        model.getObscuration(time.toKotlinInstant(), location)
}
