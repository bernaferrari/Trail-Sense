package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import kotlinx.coroutines.CoroutineScope

internal expect fun startCameraFrameAnalysis(
    camera: CameraController,
    scope: CoroutineScope,
    onFrame: (width: Int, height: Int, argb: IntArray) -> Unit,
    onError: (Throwable) -> Unit,
): AutoCloseable
