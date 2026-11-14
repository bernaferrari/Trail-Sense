package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.camera.ar.CameraCalibrationSource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.time.Duration

public enum class CameraFlash { Off, On, Auto }

/** Current zoom ratio and the supported ratio range. */
public data class CameraZoom(val ratio: Float, val minimum: Float, val maximum: Float)

/** Exposure compensation in EV; [step] is null for continuous controls. */
public data class CameraExposure(val value: Float, val minimum: Float, val maximum: Float, val step: Float? = null)

/** Supported manual exposure duration and ISO ranges. */
public data class ManualExposureRange(val duration: ClosedRange<Duration>, val iso: IntRange)

/** Independently configurable optical and preview video stabilization. */
public data class CameraStabilizationSupport(val optical: Boolean, val preview: Boolean)

/** An analysis frame with row-major ARGB pixels. Rotation is clockwise degrees when reported. */
public class CameraFrame(
    public val width: Int,
    public val height: Int,
    public val argb: IntArray,
    public val timestamp: Instant,
    public val rotationDegrees: Int? = null,
)

public interface PhotoCamera {
    /** Frames are available when [CameraSurface] enables frame analysis. */
    public val frames: Flow<CameraFrame>
    /** Sensor calibration metadata, when available. */
    public val calibration: CameraCalibrationSource?
    /** Current zoom information, or null when the platform cannot report it. */
    public val zoom: CameraZoom?
    /** Exposure compensation capabilities, or null when unavailable. */
    public val exposure: CameraExposure?
    /** Sets exposure compensation in EV, rounded to the nearest supported step where necessary. */
    public fun setExposure(value: Float)
    /** Manual shutter and ISO ranges, or null when the camera cannot set both. */
    public val manualExposure: ManualExposureRange?
    /** Sets shutter duration and ISO together; pass null for both to resume auto exposure. */
    public fun setManualExposure(duration: Duration?, iso: Int?)
    /** Whether this camera supports manually positioning its focus lens. */
    public val manualFocusSupported: Boolean
    /** Sets a lens position from 0 (near) to 1 (far), or null to resume autofocus. */
    public fun setFocus(position: Float?)
    /** Whether focus or exposure metering can be targeted within the preview. */
    public val meteringSupported: Boolean
    /** Targets a preview point using normalized coordinates (0..1), measured from the top left. */
    public fun focusAt(x: Float, y: Float)
    /** Clears targeted focus and metering. */
    public fun resetFocusAndMetering()
    /** Stabilization controls available for the current camera and preview. */
    public val stabilizationSupport: CameraStabilizationSupport
    /** Enables or disables the supported optical and preview stabilization controls. */
    public fun setStabilization(optical: Boolean, preview: Boolean)
    /** Captures an encoded image. */
    public suspend fun capture(): ByteArray
    public fun setZoom(ratio: Float)
    public fun setTorch(enabled: Boolean)
    public fun setFlash(mode: CameraFlash)
    public fun flip()
}

/** Displays a camera preview. Camera permission must be granted before composition. */
@Composable
public expect fun CameraSurface(
    modifier: Modifier = Modifier,
    onReady: (PhotoCamera?) -> Unit,
    onError: (Throwable) -> Unit = {},
    frameAnalysis: Boolean = false,
)
