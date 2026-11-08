package com.kylecorry.sol.science.astronomy.eclipse.solar

import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal class SolarEclipseParameterProvider {
    private val model = SolarEclipseParameterModel()
    fun getNextSolarEclipseParameters(after: Instant): SolarEclipseParameters {
        val result = model.getNextSolarEclipseModelParameters(after.toKotlinInstant())
        return SolarEclipseParameters(
            result.maximum.toJavaInstant(), result.minDistanceFromCenter, result.umbralConeRadius
        )
    }
}
