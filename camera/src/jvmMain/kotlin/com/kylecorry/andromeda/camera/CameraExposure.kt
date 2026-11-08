package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController

internal actual fun cameraExposure(camera: CameraController): CameraExposure? = null
internal actual fun setCameraExposure(camera: CameraController, value: Float): Unit =
    throw UnsupportedOperationException("Exposure compensation is unavailable on desktop")
