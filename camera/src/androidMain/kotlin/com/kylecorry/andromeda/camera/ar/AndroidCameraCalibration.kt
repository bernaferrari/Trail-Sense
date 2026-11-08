package com.kylecorry.andromeda.camera.ar

import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.Camera

/** Adapts Camera2 metadata to the shared calibrated projection. */
fun ICamera.calibrationSource(): CameraCalibrationSource = object : CameraCalibrationSource {
    override val calibrationId: String? get() = (this@calibrationSource as? Camera)?.getCameraId()
    override val sensorRotationDegrees: Int get() = sensorRotation.toInt()
    override val zoomRatio: Float? get() = zoom?.ratio
    override fun intrinsicCalibration(onlyUseEstimated: Boolean): FloatArray? =
        getIntrinsicCalibration(estimateIfUnavailable = true, onlyUseEstimated = onlyUseEstimated)
    override fun activeArray(preCorrection: Boolean): ImageBounds? =
        getActiveArraySize(preCorrection)?.let { ImageBounds(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
    override fun distortionCorrection(): FloatArray? = getDistortionCorrection()
}
