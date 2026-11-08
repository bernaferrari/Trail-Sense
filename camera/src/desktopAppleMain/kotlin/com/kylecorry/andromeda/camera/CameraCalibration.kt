package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import com.kylecorry.andromeda.camera.ar.CameraCalibrationSource

internal expect fun cameraCalibrationSource(camera: CameraController): CameraCalibrationSource?
