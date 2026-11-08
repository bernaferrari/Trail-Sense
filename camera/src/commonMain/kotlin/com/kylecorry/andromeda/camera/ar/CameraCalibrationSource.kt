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

/** Estimates centered pinhole intrinsics from a horizontal field of view in degrees. */
internal fun estimateCameraIntrinsics(width: Int, height: Int, horizontalFov: Float): FloatArray? {
    if (width <= 0 || height <= 0 || !horizontalFov.isFinite() || horizontalFov <= 0f || horizontalFov >= 180f) return null
    val focalLength = (width / (2.0 * kotlin.math.tan(horizontalFov * kotlin.math.PI / 360.0))).toFloat()
    if (!focalLength.isFinite()) return null
    return floatArrayOf(focalLength, focalLength, width / 2f, height / 2f, 0f)
}

/**
 * Field of view in degrees for the current zoom, derived from available pinhole intrinsics.
 * The result follows [CameraCalibrationSource.sensorRotationDegrees] and ignores lens distortion.
 */
fun CameraCalibrationSource.fieldOfView(): com.kylecorry.sol.math.geometry.Size? {
    val intrinsic = intrinsicCalibration() ?: return null
    val bounds = activeArray() ?: return null
    if (intrinsic.size < 4 || !bounds.left.isFinite() || !bounds.top.isFinite() ||
        !bounds.right.isFinite() || !bounds.bottom.isFinite() || bounds.width <= 0f || bounds.height <= 0f) return null
    val rotated = ((sensorRotationDegrees % 180) + 180) % 180 == 90
    val fx = if (rotated) intrinsic[1] else intrinsic[0]
    val fy = if (rotated) intrinsic[0] else intrinsic[1]
    val cx = if (rotated) intrinsic[3] else intrinsic[2]
    val cy = if (rotated) intrinsic[2] else intrinsic[3]
    val left = if (rotated) bounds.top else bounds.left
    val right = if (rotated) bounds.bottom else bounds.right
    val top = if (rotated) bounds.left else bounds.top
    val bottom = if (rotated) bounds.right else bounds.bottom
    val zoom = zoomRatio ?: 1f
    if (!fx.isFinite() || !fy.isFinite() || fx <= 0f || fy <= 0f ||
        !cx.isFinite() || !cy.isFinite() || !zoom.isFinite() || zoom <= 0f) return null
    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f
    val halfWidth = (right - left) / (2f * zoom)
    val halfHeight = (bottom - top) / (2f * zoom)
    val horizontal = kotlin.math.atan((centerX + halfWidth - cx) / fx) -
        kotlin.math.atan((centerX - halfWidth - cx) / fx)
    val vertical = kotlin.math.atan((centerY + halfHeight - cy) / fy) -
        kotlin.math.atan((centerY - halfHeight - cy) / fy)
    val degrees = (180.0 / kotlin.math.PI).toFloat()
    return com.kylecorry.sol.math.geometry.Size(horizontal * degrees, vertical * degrees)
}
