package com.kylecorry.sol.science.optics

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

class OpticsTest {

    @Test
    fun perspectiveProjection() {
        perspectiveProjection(0f, 0f, 0f, 0f, 0f, 0f, 0f, Float.NaN, Float.NaN)
        perspectiveProjection(0f, 0f, 0f, 1000f, 1000f, 500f, 500f, Float.NaN, Float.NaN)
        perspectiveProjection(0f, 0f, 1f, 1000f, 1000f, 500f, 500f, 500f, 500f)
        perspectiveProjection(1f, 2f, 3f, 1000f, 1000f, 500f, 500f, 833.333f, 1166.666f)
        perspectiveProjection(1f, 2f, 1f, 1000f, 1000f, 500f, 500f, 1500f, 2500f)
        perspectiveProjection(1f, 2f, 2f, 1000f, 2000f, 500f, 600f, 1000f, 2600f)
        perspectiveProjection(-1f, -2f, 3f, 1000f, 1000f, 500f, 500f, 166.666f, -166.666f)
        perspectiveProjection(-1f, -2f, 1f, 1000f, 1000f, 500f, 500f, -500f, -1500f)
        perspectiveProjection(-1f, -2f, 2f, 1000f, 2000f, 500f, 600f, 0f, -1400f)
        perspectiveProjection(-1f, -2f, -1f, 1000f, 1000f, 500f, 500f, 1500f, 2500f)
    }

    private fun perspectiveProjection(
        x: Float,
        y: Float,
        z: Float,
        fx: Float,
        fy: Float,
        cx: Float,
        cy: Float,
        expectedX: Float,
        expectedY: Float
    ) {
        val point = Vector3(x, y, z)
        val projection = Optics.perspectiveProjection(point, Vector2(fx, fy), Vector2(cx, cy))
        assertEquals(expectedX, projection.x, 0.001f)
        assertEquals(expectedY, projection.y, 0.001f)
    }

    @Test
    fun inversePerspectiveProjection() {
        inversePerspectiveProjection(0f, 0f, 0f, 0f, 0f, 0f, 0f, Float.NaN, Float.NaN, Float.NaN)
        inversePerspectiveProjection(0f, 0f, 0f, 0f, 0f, 0f, 0f, Float.NaN, Float.NaN, Float.NaN)
        inversePerspectiveProjection(500f, 500f, 1f, 1000f, 1000f, 500f, 500f, 0f, 0f, 1f)
        inversePerspectiveProjection(833.333f, 1166.666f, 3.74165f, 1000f, 1000f, 500f, 500f, 1f, 2f, 3f)
        inversePerspectiveProjection(1500f, 2500f, 2.44948f, 1000f, 1000f, 500f, 500f, 1f, 2f, 1f)
        inversePerspectiveProjection(1000f, 2600f, 3f, 1000f, 2000f, 500f, 600f, 1f, 2f, 2f)
        inversePerspectiveProjection(166.666f, -166.666f, 3.74165f, 1000f, 1000f, 500f, 500f, -1f, -2f, 3f)
        inversePerspectiveProjection(-500f, -1500f, 2.44948f, 1000f, 1000f, 500f, 500f, -1f, -2f, 1f)
        inversePerspectiveProjection(0f, -1400f, 3f, 1000f, 2000f, 500f, 600f, -1f, -2f, 2f)
    }

    private fun inversePerspectiveProjection(
        screenX: Float,
        screenY: Float,
        distance: Float,
        fx: Float,
        fy: Float,
        cx: Float,
        cy: Float,
        expectedX: Float,
        expectedY: Float,
        expectedZ: Float
    ) {
        val expected = Vector3(expectedX, expectedY, expectedZ)
        val point = Vector2(screenX, screenY)
        val projection = Optics.inversePerspectiveProjection(point, Vector2(fx, fy), Vector2(cx, cy), distance)
        assertEquals(expected.x, projection.x, 0.001f)
        assertEquals(expected.y, projection.y, 0.001f)
        assertEquals(expected.z, projection.z, 0.001f)
    }

    @Test
    fun getAngularSize() {
        getAngularSize(0f, 0f, 0f)
        getAngularSize(1f, 1f, 53.1301f)
        getAngularSize(1f, 2f, 28.0725f)
        getAngularSize(2f, 1f, 90f)
    }

    private fun getAngularSize(diameter: Float, distance: Float, expected: Float) {
        assertEquals(expected, Optics.getAngularSize(diameter, distance), 0.001f)

        // Test the distance overload
        assertEquals(
            expected,
            Optics.getAngularSize(
                Distance.meters(diameter).convertTo(DistanceUnits.Feet),
                Distance.meters(distance).convertTo(DistanceUnits.Miles)
            ),
            0.001f
        )
    }

    @Test
    fun getFocalLengthPixels() {
        getFocalLengthPixels(1f, 2f, 1000, 500f)
        getFocalLengthPixels(2f, 1f, 1000, 2000f)
        getFocalLengthPixels(1f, 2f, 2000, 1000f)
        getFocalLengthPixels(2f, 1f, 2000, 4000f)
        getFocalLengthPixels(1f, 2f, 500, 250f)
        getFocalLengthPixels(2f, 1f, 500, 1000f)
        getFocalLengthPixels(1000f, 2000f, 2000, 1000f)
    }

    private fun getFocalLengthPixels(focalLength: Float, sensorSize: Float, sensorSizePixels: Int, expected: Float) {
        assertEquals(expected, Optics.getFocalLengthPixels(focalLength, sensorSize, sensorSizePixels), 0.001f)
    }

    @Test
    fun getFieldOfView() {
        getFieldOfView(1f, 2f, 90f)
        getFieldOfView(2f, 1f, 28.0725f)
        getFieldOfView(1f, 1f, 53.1301f)
        getFieldOfView(1000f, 2000f, 90f)
        getFieldOfView(2000f, 1000f, 28.0725f)
        getFieldOfView(1000f, 1000f, 53.1301f)
    }

    private fun getFieldOfView(focalLength: Float, sensorSize: Float, expected: Float) {
        assertEquals(expected, Optics.getFieldOfView(focalLength, sensorSize), 0.001f)
    }

    @Test
    fun getFocalLength() {
        getFocalLength(90f, 2f, 1f)
        getFocalLength(28.0725f, 1f, 2f)
        getFocalLength(53.1301f, 1f, 1f)
        getFocalLength(90f, 2000f, 1000f)
        getFocalLength(28.0725f, 1000f, 2000f)
        getFocalLength(53.1301f, 1000f, 1000f)
    }

    private fun getFocalLength(fieldOfView: Float, viewSize: Float, expected: Float) {
        assertEquals(expected, Optics.getFocalLength(fieldOfView, viewSize), 0.001f)
    }

    @Test
    fun beamDistance() {
        beamDistance(8148f, 181f)
        beamDistance(5600f, 150f)
    }

    private fun beamDistance(candela: Float, distanceMeters: Float) {
        val beamDistance = Optics.lightBeamDistance(candela)
        assertEquals(distanceMeters, beamDistance.value, 0.5f)
        assertEquals(DistanceUnits.Meters, beamDistance.units)
    }

    @Test
    fun luxAtDistance() {
        luxAtDistance(8148f, 181f, 0.25f)
        luxAtDistance(5600f, 150f, 0.25f)
    }

    private fun luxAtDistance(candela: Float, meters: Float, lux: Float) {
        val distance = Distance.meters(meters)
        val actualLux = Optics.luxAtDistance(candela, distance)
        assertEquals(lux, actualLux, 0.1f)
    }

}