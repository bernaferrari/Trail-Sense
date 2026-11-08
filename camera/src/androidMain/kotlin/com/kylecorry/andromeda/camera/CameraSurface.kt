package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream

@Composable
actual fun CameraSurface(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val preview = rememberCameraPreviewState()
    val currentReady = rememberUpdatedState(onReady)
    val currentError = rememberUpdatedState(onError)
    val camera = remember(context, owner, preview) {
        Camera(context, owner, previewState = preview, analyze = false, captureSettings = ImageCaptureSettings())
    }
    val controller = remember(camera) {
        object : PhotoCamera {
            override suspend fun capture(): ByteArray {
                val output = ByteArrayOutputStream()
                check(camera.takePhoto(output)) { "Camera capture failed" }
                return output.toByteArray()
            }
            override fun setZoom(ratio: Float) = camera.setZoomRatio(ratio)
            override fun setTorch(enabled: Boolean) = camera.setTorch(enabled)
        }
    }
    DisposableEffect(camera) {
        val listener: () -> Boolean = { currentReady.value(controller); true }
        try { camera.start(listener) } catch (error: Exception) { currentError.value(error) }
        onDispose { camera.stop(null); currentReady.value(null) }
    }
    CameraPreview(preview, modifier)
}
