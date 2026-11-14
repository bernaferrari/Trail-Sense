package com.kylecorry.andromeda.camera

import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.util.SizeF
import androidx.camera.core.ImageProxy
import com.kylecorry.andromeda.core.annotations.ExperimentalUsage
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import java.io.File
import java.io.OutputStream
import kotlin.time.Duration

public interface Camera : Sensor {
    public val image: ImageProxy?
    public val zoom: ZoomInfo?
    public val sensorRotation: Float
    public fun setLinearZoom(zoom: Float)
    public fun setZoomRatio(zoom: Float)

    public fun setExposure(index: Int)
    public fun setTorch(isOn: Boolean)

    public fun flipCamera()

    /**
     * Get the field of view of the camera in degrees (horizontal, vertical) if available.
     * The FOV direction is based on when the phone is in portrait mode.
     *
     * Note: The chosen camera will be the first that matches the orientation of this camera. This should correspond to the zoom ratio of 1.
     */
    public fun getFOV(): Pair<Float, Float>?

    /**
     * Get the field of view of the camera in degrees (horizontal, vertical) adjusted by the zoom ratio if available.
     * The FOV direction is based on when the phone is in portrait mode.
     *
     * Note: The chosen camera will be the first that matches the orientation of this camera.
     */
    public fun getZoomedFOV(): Pair<Float, Float>?

    public fun stopFocusAndMetering()
    public fun startFocusAndMetering(point: PixelCoordinate)

    public fun takePhoto(callback: (image: ImageProxy?) -> Unit)
    public suspend fun takePhoto(): ImageProxy?
    public suspend fun takePhoto(file: File): Boolean
    public suspend fun takePhoto(stream: OutputStream): Boolean

    /**
     * Set the stabilization mode of the camera. This may not be supported on all devices.
     * @param opticalStabilization true to enable optical stabilization, false to disable
     * @param videoStabilization true to enable video stabilization, false to disable
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun setStabilization(opticalStabilization: Boolean, videoStabilization: Boolean)

    /**
     * Set the focus distance of the camera as a percentage of the focus range [0, 1]. This may not be supported on all devices.
     * @param distance the distance to focus to as a percentage of the focus range [0, 1]
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun setFocusDistancePercentage(distance: Float?)

    /**
     * Set the exposure time of the camera. This may not be supported on all devices.
     * @param exposureTime the exposure time to set
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun setExposureTime(exposureTime: Duration?)

    /**
     * Set the sensitivity of the camera. This may not be supported on all devices.
     * @param isoSensitivity the sensitivity to set (ISO)
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun setSensitivity(isoSensitivity: Int?)

    /**
     * Get the supported exposure time range of the camera. This may not be supported on all devices.
     * @return the exposure time range or null if not supported
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun getExposureTimeRange(): Range<Duration>?

    /**
     * Get the supported sensitivity range of the camera. This may not be supported on all devices.
     * @return the sensitivity range (ISO) or null if not supported
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun getSensitivityRange(): Range<Int>?

    /**
     * Get the supported exposure compensation range of the camera. This may not be supported on all devices.
     * @return the exposure compensation range or null if not supported
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun getExposureCompensationRange(): Range<Int>?

    /**
     * Determines if optical stabilization is supported.
     * @return true if optical stabilization is supported
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun isOpticalStabilizationSupported(): Boolean

    /**
     * Determines if video stabilization is supported.
     * @return true if video stabilization is supported
     */
    @ExperimentalUsage("This uses unstable APIs and may not work on all devices")
    public fun isVideoStabilizationSupported(): Boolean

    /**
     * Get the size of the camera preview.
     * @param cropToView true to crop the preview to the view, false to include the entire preview
     * @return the size of the preview or null if not available
     */
    public fun getPreviewSize(cropToView: Boolean = true): Size?

    /**
     * Get the rect of the camera preview.
     * @param cropToView true to crop the preview to the view, false to include the entire preview
     * @return the rect of the preview or null if not available
     */
    public fun getPreviewRect(cropToView: Boolean = true): RectF?

    /**
     * Get the field of view of the camera preview in degrees (horizontal, vertical)
     * @param cropToView true to crop the preview to the view, false to include the entire preview
     * @return the FOV of the preview or null if not available
     */
    public fun getPreviewFOV(cropToView: Boolean): Pair<Float, Float>?

    /**
     * Get the intrinsic calibration of the camera.
     * It will be an array of [fx, fy, cx, cy, skew] in active array pixels.
     * @param estimateIfUnavailable if true and the manufacturer calibration is not available, the calibration will be estimated. Defaults to true.
     * @param onlyUseEstimated if true, only the estimated calibration will be used. Defaults to false.
     * @return the intrinsic calibration or null if not available
     */
    public fun getIntrinsicCalibration(
        estimateIfUnavailable: Boolean = true,
        onlyUseEstimated: Boolean = false
    ): FloatArray?

    /**
     * Get the active array size of the camera in pixels.
     * @param preCorrection true to get the active array size before lens correction, false to get the active array size after lens correction
     * @return the active array size or null if not available
     */
    public fun getActiveArraySize(preCorrection: Boolean = false): Rect?

    /**
     * Get the full array size of the camera in pixels.
     * @return the full array size or null if not available
     */
    public fun getFullArraySize(): Size?

    /**
     * Get the distortion correction of the camera.
     * It will be an array of [k1, k2, k3, k4, k5] or null if not available.
     * @return the distortion correction or null if not available
     */
    public fun getDistortionCorrection(): FloatArray?

    /**
     * Get the sensor size of the camera in millimeters.
     * @return the sensor size or null if not available
     */
    public fun getSensorSize(): SizeF?

    /**
     * Get the focal length of the camera in millimeters.
     * @return the focal length or null if not available
     */
    public fun getFocalLength(): Float?
}
