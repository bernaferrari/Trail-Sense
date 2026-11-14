package com.kylecorry.sol.math

import com.kylecorry.sol.math.interpolation.LocalNewtonInterpolator
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class PortableMathTest {
    @Test
    fun angleAndDistanceConversions() {
        assertEquals(PI, Trigonometry.toRadians(180.0), 1e-12)
        assertEquals(180.0, Trigonometry.toDegrees(PI), 1e-12)
        assertEquals(1609.344f, Distance.miles(1f).convertTo(DistanceUnits.Meters).value, 0.001f)
    }

    @Test
    fun localInterpolationCanReuseAndReplaceItsCache() {
        val interpolator = LocalNewtonInterpolator(
            listOf(Vector2(0f, 0f), Vector2(1f, 1f), Vector2(2f, 4f), Vector2(3f, 9f)),
            2
        )
        assertEquals(0.25f, interpolator.interpolate(0.5f), 0.0001f)
        assertEquals(0.25f, interpolator.interpolate(0.5f), 0.0001f)
        assertEquals(6.25f, interpolator.interpolate(2.5f), 0.0001f)
    }
}
