package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController

internal expect fun cameraExposure(camera: CameraController): CameraExposure?
internal expect fun setCameraExposure(camera: CameraController, value: Float)

internal expect fun cameraManualFocusSupported(camera: CameraController): Boolean
internal expect fun setCameraFocus(camera: CameraController, position: Float?)
