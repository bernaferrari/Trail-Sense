package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController

internal actual fun cameraExposure(camera: CameraController): CameraExposure? = null
internal actual fun setCameraExposure(camera: CameraController, value: Float): Unit =
    throw UnsupportedOperationException("Exposure compensation is unavailable on desktop")

internal actual fun cameraManualFocusSupported(camera: CameraController): Boolean = false
internal actual fun setCameraFocus(camera: CameraController, position: Float?): Unit =
    throw UnsupportedOperationException("Focus control is unavailable on desktop")
