package com.kylecorry.andromeda.camera.ar

import com.kylecorry.sol.math.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class CalibratedCameraAnglePixelMapperTest {
    @Test
    fun projectsCalibrationAndTracksZoom() {
        val source = object : CameraCalibrationSource {
            override var calibrationId: String? = "first"
            var focalLength = 100f
            override val sensorRotationDegrees = 0
            override var zoomRatio: Float? = 1f
            override fun intrinsicCalibration(onlyUseEstimated: Boolean) = floatArrayOf(focalLength, focalLength, 50f, 50f, 0f)
            override fun activeArray(preCorrection: Boolean) = ImageBounds(0f, 0f, 100f, 100f)
            override fun distortionCorrection(): FloatArray? = null
        }
        val mapper = CalibratedCameraAnglePixelMapper(source)
        val bounds = ImageBounds(0f, 0f, 200f, 200f)
        val fov = Size(90f, 90f)
        val center = mapper.getPixel(0f, 0f, bounds, fov)
        assertEquals(100f, center.x, 0.001f)
        assertEquals(100f, center.y, 0.001f)
        val side = mapper.getPixel(45f, 0f, bounds, fov)
        assertEquals(300f, side.x, 0.001f)
        source.zoomRatio = 2f
        assertEquals(500f, mapper.getPixel(45f, 0f, bounds, fov).x, 0.001f)
        source.focalLength = 50f
        source.calibrationId = "second"
        assertEquals(300f, mapper.getPixel(45f, 0f, bounds, fov).x, 0.001f)
    }
}
