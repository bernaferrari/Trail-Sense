package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapRotationService
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PhotoMapRotationServiceTest {

    private data class Case(val x: Float, val y: Float, val rotation: Float, val expectedX: Float, val expectedY: Float)

    @Test
    fun getCalibrationPoints() {
        val cases = listOf(
            // Point is centered, rotation should not affect it
            Case(0.5f, 0.5f, 0f, 0.5f, 0.5f),
            Case(0.5f, 0.5f, 90f, 0.5f, 0.5f),
            Case(0.5f, 0.5f, 180f, 0.5f, 0.5f),
            Case(0.5f, 0.5f, 270f, 0.5f, 0.5f),
            // Point in corner
            Case(0f, 0f, 0f, 0f, 0f),
            Case(0f, 0f, 90f, 1f, 0f),
            Case(0f, 0f, 180f, 1f, 1f),
            Case(0f, 0f, 270f, 0f, 1f),
            // Point in opposite corner
            Case(1f, 1f, 0f, 1f, 1f),
            Case(1f, 1f, 90f, 0f, 1f),
            Case(1f, 1f, 180f, 0f, 0f),
            Case(1f, 1f, 270f, 1f, 0f),
            // Point in corner, 45 rotation
            Case(0f, 0f, 45f, 0.666667f, 0f),
            Case(0f, 0f, 135f, 1.0f, 0.666667f),
            Case(0f, 0f, 225f, 0.333333f, 1f),
            Case(0f, 0f, 315f, 0.0f, 0.333333f),
            // Point not in corner
            Case(0.25f, 0.8f, 45f, 0.2166667f, 0.6166667f),
            Case(0.25f, 0.8f, 90f, 0.2f, 0.25f),
            Case(0.25f, 0.8f, 135f, 0.38333333f, 0.21666667f),
            Case(0.25f, 0.8f, 180f, 0.75f, 0.2f),
            Case(0.25f, 0.8f, 225f, 0.78333333f, 0.38333333f),
            Case(0.25f, 0.8f, 270f, 0.8f, 0.75f),
            Case(0.25f, 0.8f, 315f, 0.61666667f, 0.78333333f)
        )
        for (case in cases) {
            val map = PhotoMap(
                0,
                "",
                "",
                0,
                PhotoMapGeoreference(
                    Size(100f, 200f),
                    isWarpingCompleted = true,
                    rotation = case.rotation,
                    calibrationPoints = listOf(
                        MapCalibrationPoint(
                            Coordinate(0.0, 0.0),
                            PercentCoordinate(case.x, case.y)
                        )
                    )
                )
            )

            val points = PhotoMapRotationService(map).getCalibrationPoints()

            assertEquals(case.expectedX, points[0].imageLocation.x, 0.0001f, "x for $case")
            assertEquals(case.expectedY, points[0].imageLocation.y, 0.0001f, "y for $case")
        }
    }
}
