package com.kylecorry.trail_sense.tools.paths.domain.waypointcolors

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RangePointColoringStrategyTest {

    // Reports the ratio as a percentage so the tests can read where a point fell in the range.
    private val percent = object : IColorScale {
        override fun getColor(percent: Float): Int = (percent * 100).roundToInt()
    }

    @Test
    fun altitudeIsPlacedInTheRangeAndClamped() {
        val strategy = AltitudePointColoringStrategy(100f..200f, percent)
        assertEquals(0, strategy.getColor(point(elevation = 100f)))
        assertEquals(25, strategy.getColor(point(elevation = 125f)))
        assertEquals(100, strategy.getColor(point(elevation = 300f)))
        assertEquals(0, strategy.getColor(point(elevation = 50f)))
        assertNull(strategy.getColor(point()))
    }

    @Test
    fun timeIsPlacedInTheRangeAndClamped() {
        val start = Instant.fromEpochSeconds(1_000)
        val end = Instant.fromEpochSeconds(2_000)
        val strategy = TimePointColoringStrategy(start..end, percent)
        assertEquals(0, strategy.getColor(point(time = start)))
        assertEquals(50, strategy.getColor(point(time = Instant.fromEpochSeconds(1_500))))
        assertEquals(100, strategy.getColor(point(time = Instant.fromEpochSeconds(5_000))))
        assertNull(strategy.getColor(point()))
    }

    private fun point(elevation: Float? = null, time: Instant? = null): PathPoint {
        return PathPoint(0, 0, Coordinate.zero, elevation, time)
    }
}
