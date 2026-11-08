package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController

internal expect fun cameraExposure(camera: CameraController): CameraExposure?
internal expect fun setCameraExposure(camera: CameraController, value: Float)

internal expect fun cameraManualFocusSupported(camera: CameraController): Boolean
internal expect fun setCameraFocus(camera: CameraController, position: Float?)

internal expect fun cameraManualExposure(camera: CameraController): ManualExposureRange?
internal expect fun setCameraManualExposure(camera: CameraController, duration: kotlin.time.Duration?, iso: Int?)
