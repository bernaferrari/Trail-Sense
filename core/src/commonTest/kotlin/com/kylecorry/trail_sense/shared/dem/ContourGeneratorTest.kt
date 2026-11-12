package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.sol.shared.Executor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContourGeneratorTest {

    private val sequential = object : Executor {
        override fun <T> map(tasks: List<() -> T>): List<T> = tasks.map { it() }
        override fun run(tasks: List<() -> Unit>) = tasks.forEach { it() }
    }

    @Test
    fun aSlopeGetsOneContourPerIntervalCrossed() = runTest {
        // Elevation rises 10 m per column from 0 to 40 m
        val latitudes = doubleArrayOf(0.0, 0.01, 0.02)
        val longitudes = doubleArrayOf(0.0, 0.01, 0.02, 0.03, 0.04)
        val contours = ContourGenerator.generate(latitudes, longitudes, 10f, sequential) { x, _ -> x * 10f }

        assertEquals(listOf(0f, 10f, 20f, 30f, 40f), contours.map { it.elevation })
        // The interior levels cross the whole grid, north to south, as one line near their column
        for (level in listOf(10f, 20f, 30f)) {
            val lines = contours.first { it.elevation == level }.lines
            assertEquals(1, lines.size)
            val column = level / 10f * 0.01
            assertTrue(lines[0].all { kotlin.math.abs(it.longitude - column) < 0.001 })
        }
    }

    @Test
    fun belowSeaLevelGridsOnlyGetTheirOwnLevels() = runTest {
        // Every elevation is negative (ex. the Dead Sea shore); no levels above the highest point
        val latitudes = doubleArrayOf(0.0, 0.01)
        val longitudes = doubleArrayOf(0.0, 0.01, 0.02)
        val contours = ContourGenerator.generate(latitudes, longitudes, 10f, sequential) { x, _ -> -430f + x * 10f }

        assertEquals(listOf(-430f, -420f, -410f), contours.map { it.elevation })
    }

    @Test
    fun anEmptyGridHasNoContours() = runTest {
        val contours = ContourGenerator.generate(doubleArrayOf(), doubleArrayOf(), 10f, sequential) { _, _ -> 0f }
        assertTrue(contours.isEmpty())
    }

    @Test
    fun waterMaskValuesMapToWaterTypes() {
        assertEquals(DEMElevationType.Land, DEMElevationType.fromWaterMask(0f))
        assertEquals(DEMElevationType.Land, DEMElevationType.fromWaterMask(63.9f))
        assertEquals(DEMElevationType.InlandWater, DEMElevationType.fromWaterMask(64f))
        assertEquals(DEMElevationType.InlandWater, DEMElevationType.fromWaterMask(191f))
        assertEquals(DEMElevationType.Ocean, DEMElevationType.fromWaterMask(192f))
        assertEquals(DEMElevationType.Ocean, DEMElevationType.fromWaterMask(255f))
    }
}
