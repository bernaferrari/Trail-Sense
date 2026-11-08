package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.camera.ar.CameraCalibrationSource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

enum class CameraFlash { Off, On, Auto }

/** Current zoom ratio and the supported ratio range. */
data class CameraZoom(val ratio: Float, val minimum: Float, val maximum: Float)

/** Exposure compensation in EV; [step] is null for continuous controls. */
data class CameraExposure(val value: Float, val minimum: Float, val maximum: Float, val step: Float? = null)

/** An analysis frame with row-major ARGB pixels. Rotation is clockwise degrees when reported. */
class CameraFrame(
    val width: Int,
    val height: Int,
    val argb: IntArray,
    val timestamp: Instant,
    val rotationDegrees: Int? = null,
)

interface PhotoCamera {
    /** Frames are available when [CameraSurface] enables frame analysis. */
    val frames: Flow<CameraFrame>
    /** Sensor calibration metadata, when available. */
    val calibration: CameraCalibrationSource?
    /** Current zoom information, or null when the platform cannot report it. */
    val zoom: CameraZoom?
    /** Exposure compensation capabilities, or null when unavailable. */
    val exposure: CameraExposure?
    /** Sets exposure compensation in EV, rounded to the nearest supported step where necessary. */
    fun setExposure(value: Float)
    /** Captures an encoded image. */
    suspend fun capture(): ByteArray
    fun setZoom(ratio: Float)
    fun setTorch(enabled: Boolean)
    fun setFlash(mode: CameraFlash)
    fun flip()
}

/** Displays a camera preview. Camera permission must be granted before composition. */
@Composable
expect fun CameraSurface(
    modifier: Modifier = Modifier,
    onReady: (PhotoCamera?) -> Unit,
    onError: (Throwable) -> Unit = {},
    frameAnalysis: Boolean = false,
)
