package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.camera.ar.CameraCalibrationSource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

enum class CameraFlash { Off, On, Auto }

/** Current zoom ratio and the supported ratio range. */
data class CameraZoom(val ratio: Float, val minimum: Float, val maximum: Float)

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
