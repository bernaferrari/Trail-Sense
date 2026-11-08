package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

@Composable
internal fun CameraSurfaceImpl(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit, frameAnalysis: Boolean) {
    val configuration = remember { CameraConfiguration(directory = Directory.DOCUMENTS) }
    val state by rememberCameraKState(configuration)
    val ready = state as? CameraKState.Ready
    val currentReady = rememberUpdatedState(onReady)
    val currentError = rememberUpdatedState(onError)
    val controller = ready?.controller
    val scope = rememberCoroutineScope()
    val frames = remember(controller, frameAnalysis) {
        MutableSharedFlow<CameraFrame>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    LaunchedEffect(controller, frameAnalysis, frames) {
        if (controller != null && frameAnalysis) frames.subscriptionCount.collectLatest { count ->
            if (count > 0) {
                try {
                    val analyzer = startCameraFrameAnalysis(
                        controller, scope,
                        onFrame = { width, height, argb -> frames.tryEmit(CameraFrame(width, height, argb, Clock.System.now())) },
                        onError = { error -> currentError.value(error) },
                    )
                    try { awaitCancellation() } finally { analyzer.close() }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    currentError.value(error)
                }
            }
        }
    }
    val adapter = remember(controller, frameAnalysis, frames) {
        controller?.let { camera ->
            object : PhotoCamera {
                override val frames = if (frameAnalysis) frames.asSharedFlow() else flow { error("Frame analysis is disabled") }
                override val calibration = cameraCalibrationSource(camera)
                override val zoom: CameraZoom
                    get() {
                        val ratio = camera.getZoom()
                        val maximum = camera.getMaxZoom()
                        val lens = camera.getCameraLens()
                        val deviceType = camera.getPreferredCameraDeviceType()
                        val minimum = camera.getCameraCapabilities().allLenses
                            .firstOrNull { it.lens == lens && it.deviceType == deviceType }?.minZoom ?: 1f
                        return CameraZoom(ratio, minimum, maximum)
                    }
                override val exposure: CameraExposure? get() = cameraExposure(camera)
                override fun setExposure(value: Float) = setCameraExposure(camera, value)
                override suspend fun capture(): ByteArray = when (val result = camera.takePictureToFile()) {
                    is ImageCaptureResult.Success -> result.byteArray
                    is ImageCaptureResult.SuccessWithFile -> {
                        val path = result.filePath.toPath()
                        try { FileSystem.SYSTEM.source(path).buffer().use { it.readByteArray() } }
                        finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
                    }
                    is ImageCaptureResult.Error -> throw result.exception
                }
                override fun setZoom(ratio: Float) {
                    require(ratio.isFinite() && ratio > 0f)
                    camera.setZoom(ratio)
                }
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
