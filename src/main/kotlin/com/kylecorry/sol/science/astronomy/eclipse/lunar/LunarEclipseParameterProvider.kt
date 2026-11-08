package com.kylecorry.sol.science.astronomy.eclipse.lunar

import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal class LunarEclipseParameterProvider {
    private val model = LunarEclipseParameterModel()
    fun getNextLunarEclipseParameters(after: Instant): LunarEclipseParameters {
        val result = model.getNextLunarEclipseModelParameters(after.toKotlinInstant())
        return LunarEclipseParameters(
            result.maximum.toJavaInstant(), result.minDistanceFromCenter, result.umbralConeRadius, result.n
        )
    }
}
