package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController

internal actual fun cameraExposure(camera: CameraController): CameraExposure? = null
internal actual fun setCameraExposure(camera: CameraController, value: Float): Unit =
    throw UnsupportedOperationException("Exposure compensation is unavailable on desktop")

internal actual fun cameraManualFocusSupported(camera: CameraController): Boolean = false
internal actual fun setCameraFocus(camera: CameraController, position: Float?): Unit =
    throw UnsupportedOperationException("Focus control is unavailable on desktop")

internal actual fun cameraManualExposure(camera: CameraController): ManualExposureRange? = null
internal actual fun setCameraManualExposure(camera: CameraController, duration: kotlin.time.Duration?, iso: Int?): Unit =
    throw UnsupportedOperationException("Manual exposure is unavailable on desktop")

internal actual fun cameraMeteringSupported(camera: CameraController): Boolean = false
internal actual fun focusCameraAt(camera: CameraController, x: Float, y: Float): Unit =
    throw UnsupportedOperationException("Focus and metering are unavailable on desktop")
internal actual fun resetCameraMetering(camera: CameraController): Unit =
    throw UnsupportedOperationException("Focus and metering are unavailable on desktop")

internal actual fun cameraStabilizationSupport(camera: CameraController): CameraStabilizationSupport =
    CameraStabilizationSupport(optical = false, preview = false)
internal actual fun setCameraStabilization(camera: CameraController, optical: Boolean, preview: Boolean) {
    require(!optical && !preview) { "Camera stabilization is unavailable on desktop" }
}
