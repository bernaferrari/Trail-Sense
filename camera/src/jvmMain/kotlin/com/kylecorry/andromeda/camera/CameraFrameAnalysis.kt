package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import java.awt.image.BufferedImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual fun startCameraFrameAnalysis(
    camera: CameraController,
    scope: CoroutineScope,
    onFrame: (width: Int, height: Int, argb: IntArray) -> Unit,
    onError: (Throwable) -> Unit,
): AutoCloseable {
    val job = scope.launch(Dispatchers.Default) {
        try {
            camera.frameFlow.collect { image ->
                onFrame(image.width, image.height, cameraFramePixels(image))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            withContext(Dispatchers.Main) { onError(error) }
        }
    }
    return AutoCloseable { job.cancel() }
}

internal fun cameraFramePixels(image: BufferedImage): IntArray =
    image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
