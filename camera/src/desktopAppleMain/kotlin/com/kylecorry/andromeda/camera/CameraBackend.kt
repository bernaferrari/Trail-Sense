package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.kashif.cameraK.compose.CameraPreviewView
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraKState
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.state.CameraConfiguration
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

@Composable
internal fun CameraSurfaceImpl(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit) {
    val configuration = remember { CameraConfiguration(directory = Directory.DOCUMENTS) }
    val state by rememberCameraKState(configuration)
    val ready = state as? CameraKState.Ready
    val currentReady = rememberUpdatedState(onReady)
    val currentError = rememberUpdatedState(onError)
    val controller = ready?.controller
    val adapter = remember(controller) {
        controller?.let { camera ->
            object : PhotoCamera {
                override suspend fun capture(): ByteArray = when (val result = camera.takePictureToFile()) {
                    is ImageCaptureResult.Success -> result.byteArray
                    is ImageCaptureResult.SuccessWithFile -> {
                        val path = result.filePath.toPath()
                        try { FileSystem.SYSTEM.source(path).buffer().use { it.readByteArray() } }
                        finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
                    }
                    is ImageCaptureResult.Error -> throw result.exception
                }
                override fun setZoom(ratio: Float) = camera.setZoom(ratio)
                override fun setTorch(enabled: Boolean) = camera.setTorchMode(if (enabled) TorchMode.ON else TorchMode.OFF)
                override fun setFlash(mode: CameraFlash) = camera.setFlashMode(when (mode) {
                    CameraFlash.Off -> FlashMode.OFF
                    CameraFlash.On -> FlashMode.ON
                    CameraFlash.Auto -> FlashMode.AUTO
                })
                override fun flip() = camera.toggleCameraLens()
            }
        }
    }
    LaunchedEffect(adapter, state) {
        currentReady.value(adapter)
        (state as? CameraKState.Error)?.let { currentError.value(it.exception) }
    }
    DisposableEffect(Unit) { onDispose { currentReady.value(null) } }
    if (controller != null) CameraPreviewView(controller, modifier)
}
