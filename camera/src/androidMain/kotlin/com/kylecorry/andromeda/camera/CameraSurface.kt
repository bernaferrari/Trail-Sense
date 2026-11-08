package com.kylecorry.andromeda.camera

import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import com.kylecorry.andromeda.camera.ar.calibrationSource

@Composable
actual fun CameraSurface(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit, frameAnalysis: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val preview = rememberCameraPreviewState()
    val currentReady = rememberUpdatedState(onReady)
    val currentError = rememberUpdatedState(onError)
    val frames = remember(context, owner, preview, frameAnalysis) {
        MutableSharedFlow<CameraFrame>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val camera = remember(context, owner, preview, frameAnalysis) {
        Camera(
            context, owner, previewState = preview, analyze = frameAnalysis,
            captureSettings = ImageCaptureSettings(),
            onAnalysisFrame = if (!frameAnalysis) null else { image ->
                if (frames.subscriptionCount.value > 0) {
                    val bitmap = image.toBitmap()
                    try {
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        frames.tryEmit(CameraFrame(bitmap.width, bitmap.height, pixels, Clock.System.now(), image.imageInfo.rotationDegrees))
                    } finally { bitmap.recycle() }
                }
            },
            onAnalysisError = { error -> ContextCompat.getMainExecutor(context).execute { currentError.value(error) } },
        )
    }
    val controller = remember(camera) {
        object : PhotoCamera {
            override val frames = if (frameAnalysis) frames.asSharedFlow() else flow { error("Frame analysis is disabled") }
            override val calibration = camera.calibrationSource()
            override val zoom: CameraZoom?
                get() = camera.zoom?.let { CameraZoom(it.ratio, it.ratioRange.start, it.ratioRange.end) }
            override val exposure: CameraExposure? get() = camera.exposureCompensation
            override fun setExposure(value: Float) = camera.setExposureCompensation(value)
            override val manualExposure: ManualExposureRange? get() = camera.manualExposureRange
            override fun setManualExposure(duration: kotlin.time.Duration?, iso: Int?) = camera.setSharedManualExposure(duration, iso)
            override val manualFocusSupported: Boolean get() = camera.manualFocusSupported
            override fun setFocus(position: Float?) = camera.setSharedFocus(position)
            override suspend fun capture(): ByteArray {
                val output = ByteArrayOutputStream()
                check(camera.takePhoto(output)) { "Camera capture failed" }
                return output.toByteArray()
            }
            override fun setZoom(ratio: Float) {
                require(ratio.isFinite() && ratio > 0f)
                camera.setZoomRatio(ratio)
            }
            override fun setTorch(enabled: Boolean) = camera.setTorch(enabled)
            override fun setFlash(mode: CameraFlash) = camera.setFlashMode(when (mode) {
                CameraFlash.Off -> ImageCapture.FLASH_MODE_OFF
                CameraFlash.On -> ImageCapture.FLASH_MODE_ON
                CameraFlash.Auto -> ImageCapture.FLASH_MODE_AUTO
            })
            override fun flip() = camera.flipCamera()
        }
    }
    DisposableEffect(camera) {
        val listener: () -> Boolean = { currentReady.value(controller); true }
        try { camera.start(listener) } catch (error: Exception) { currentError.value(error) }
        onDispose { camera.stop(null); currentReady.value(null) }
    }
    CameraPreview(preview, modifier)
}
