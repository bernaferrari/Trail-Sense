package com.kylecorry.andromeda.camera.ar

/** Supplies camera calibration in active sensor pixels. Values may be unavailable on a device. */
interface CameraCalibrationSource {
    /** Changes when the active physical camera changes, if the platform can identify it. */
    val calibrationId: String? get() = null
    val sensorRotationDegrees: Int
    val zoomRatio: Float?
    fun intrinsicCalibration(onlyUseEstimated: Boolean = false): FloatArray?
    fun activeArray(preCorrection: Boolean = false): ImageBounds?
    fun distortionCorrection(): FloatArray?
}
