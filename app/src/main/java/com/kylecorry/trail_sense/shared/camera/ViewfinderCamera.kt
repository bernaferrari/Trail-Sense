package com.kylecorry.trail_sense.shared.camera

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Log
import android.util.Size
import android.util.SizeF
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.camera.ZoomInfo
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Range
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.atan

/** How the camera picture sits in its viewfinder: cropped to fill it, or whole inside it. Both are centred. */
enum class CameraScaleType {
    Fill,
    Fit
}

/** The viewfinder's size on screen and how the picture is scaled into it, for mapping preview pixels. */
data class ViewfinderLayout(val width: Int, val height: Int, val scaleType: CameraScaleType)

/**
 * A CameraX camera whose preview goes to a Compose viewfinder instead of a PreviewView: the Preview use case hands
 * its [SurfaceRequest] to [onSurfaceRequest] (shown with `CameraXViewfinder`), and the preview geometry comes from
 * [layout]. Otherwise it matches Andromeda's Camera (image analysis, capture, zoom, torch, exposure, focus, FOV and
 * intrinsics), which it was adapted from.
 */
class ViewfinderCamera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val isBackCamera: Boolean = true,
    private val onSurfaceRequest: (SurfaceRequest?) -> Unit,
    private val layout: () -> ViewfinderLayout?,
    private val toSurfacePoint: (PixelCoordinate) -> PixelCoordinate? = { null },
    private val analyze: Boolean = true,
    private val targetResolution: Size? = null,
    private val useYUV: Boolean = false,
    private val captureSettings: ImageCaptureSettings? = null,
    private val shouldStabilizePreview: Boolean = true,
    @param:AspectRatio.Ratio
    private val targetAspectRatio: Int = AspectRatio.RATIO_DEFAULT,
) : AbstractSensor(), ICamera {

    override val image: ImageProxy?
        get() = _image

    override val zoom: ZoomInfo?
        get() {
            try {
                val state = camera?.cameraInfo?.zoomState?.value ?: return null
                return ZoomInfo(
                    state.zoomRatio,
                    state.linearZoom,
                    Range(state.minZoomRatio, state.maxZoomRatio)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

    override val sensorRotation: Float
        get() {
            return try {
                camera?.cameraInfo?.sensorRotationDegrees?.toFloat() ?: 0f
            } catch (e: Exception) {
                e.printStackTrace()
                0f
            }
        }

    private var _image: ImageProxy? = null

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector? = null

    private var _hasValidReading = false

    private var preview: Preview? = null

    override val hasValidReading: Boolean
        get() = _hasValidReading

    private var cachedFOV: Pair<Float, Float>? = null
    private var cachedFullPreviewSize: Size? = null
    private var cachedCroppedPreviewSize: Size? = null

    @OptIn(ExperimentalCamera2Interop::class)
    override fun startImpl() {
        if (!Permissions.isCameraEnabled(context)) {
            return
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture?.addListener({
            try {
                cameraProvider = cameraProviderFuture?.get()
            } catch (e: CancellationException) {
                Log.i("Camera", "Unable to open camera because task was cancelled")
            } catch (e: InterruptedException) {
                Log.i("Camera", "Unable to open camera because task was interrupted")
            }
            val resolutionSelector = getResolutionSelector()
            preview = Preview.Builder()
                .also {
                    it.setResolutionSelector(resolutionSelector)

                    if (!shouldStabilizePreview) {
                        tryOrLog {
                            // https://issuetracker.google.com/issues/230013960?pli=1
                            Camera2Interop.Extender(it)
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                                )
                                .setCaptureRequestOption(
                                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                                )
                        }
                    }
                }
                .build()

            val imageAnalysis = ImageAnalysis.Builder().apply {
                setResolutionSelector(resolutionSelector)
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                setOutputImageFormat(
                    if (useYUV) {
                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
                    } else {
                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                    }
                )
            }.build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                _image = image
                _hasValidReading = true
                setEventTimeToNow()
                notifyListeners()
            }

            imageCapture = captureSettings?.let { settings ->
                val builder = ImageCapture.Builder()
                    .setFlashMode(settings.flashMode)
                    .setCaptureMode(settings.captureMode)
                settings.quality?.let { builder.setJpegQuality(it) }
                builder.setResolutionSelector(resolutionSelector)
                settings.rotation?.let { builder.setTargetRotation(it) }
                builder.build()
            }

            if (cameraSelector == null) {
                cameraSelector =
                    if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            }

            preview?.setSurfaceProvider(ContextCompat.getMainExecutor(context)) { request ->
                onSurfaceRequest(request)
            }

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA,
                *listOfNotNull(
                    preview,
                    if (analyze) imageAnalysis else null,
                    imageCapture
                ).toTypedArray()
            )

            notifyListeners()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun getResolutionSelector(): ResolutionSelector {
        val builder = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    if (targetAspectRatio == AspectRatio.RATIO_DEFAULT) {
                        AspectRatio.RATIO_4_3
                    } else {
                        targetAspectRatio
                    },
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )

        targetResolution?.let {
            builder.setResolutionStrategy(
                ResolutionStrategy(
                    it,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
        }

        return builder.build()
    }

    override fun flipCamera() {
        cameraProvider ?: return
        cameraSelector ?: return
        cameraSelector = if (cameraSelector === CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        cachedFullPreviewSize = null
        cachedCroppedPreviewSize = null
        lifecycleOwner.lifecycleScope.launch {
            stopImpl()
            startImpl()
        }
    }

    override fun stopImpl() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraProviderFuture?.cancel(true)
        cameraProviderFuture = null
        cachedFOV = null
        onSurfaceRequest(null)
    }

    override fun setLinearZoom(zoom: Float) {
        camera?.cameraControl?.setLinearZoom(zoom)
    }

    override fun setZoomRatio(zoom: Float) {
        camera?.cameraControl?.setZoomRatio(zoom)
    }

    override fun setExposure(index: Int) {
        tryOrLog {
            camera?.cameraControl?.setExposureCompensationIndex(index)
        }
    }

    override fun setTorch(isOn: Boolean) {
        camera?.cameraControl?.enableTorch(isOn)
        imageCapture?.flashMode = if (isOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    override fun stopFocusAndMetering() {
        tryOrNothing {
            camera?.cameraControl?.cancelFocusAndMetering()
        }
    }

    override fun startFocusAndMetering(point: PixelCoordinate) {
        val resolution = preview?.resolutionInfo?.resolution ?: return
        val surfacePoint = toSurfacePoint(point) ?: return
        val factory = SurfaceOrientedMeteringPointFactory(
            resolution.width.toFloat(),
            resolution.height.toFloat()
        )
        val action = FocusMeteringAction.Builder(factory.createPoint(surfacePoint.x, surfacePoint.y)).build()

        tryOrNothing {
            camera?.cameraControl?.startFocusAndMetering(action)
        }
    }

    override fun takePhoto(callback: (image: ImageProxy?) -> Unit) {
        val imageCapture = imageCapture
        if (imageCapture == null) {
            callback(null)
            return
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    callback(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    callback(null)
                }
            })
    }

    override suspend fun takePhoto(): ImageProxy? = suspendCoroutine { cont ->
        takePhoto {
            cont.resume(it)
        }
    }

    override suspend fun takePhoto(file: File): Boolean = suspendCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        takePhoto(options, cont)
    }

    override suspend fun takePhoto(stream: OutputStream): Boolean = suspendCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(stream).build()
        takePhoto(options, cont)
    }

    private fun takePhoto(options: ImageCapture.OutputFileOptions, cont: Continuation<Boolean>) {
        val imageCapture = imageCapture
        if (imageCapture == null) {
            cont.resume(false)
            return
        }
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(true)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resume(false)
                }
            })
    }

    override fun getFOV(): Pair<Float, Float>? {
        cachedFOV?.let { return it }

        try {
            val activePixelSize = getActiveArraySize(false) ?: return null
            val fullPixelSize = getFullArraySize() ?: return null
            val focalLength = getFocalLength() ?: return null
            val size = getSensorSize() ?: return null
            val sensorOrientation = sensorRotation.toInt()

            val w = size.width * activePixelSize.width() / fullPixelSize.width.toFloat()
            val h = size.height * activePixelSize.height() / fullPixelSize.height.toFloat()
            val horizontalFOV = 2 * atan(w / (2 * focalLength))
            val verticalFOV = 2 * atan(h / (2 * focalLength))

            val isFlipped = sensorOrientation == 90 || sensorOrientation == 270

            val hFOV = if (isFlipped) verticalFOV else horizontalFOV
            val vFOV = if (isFlipped) horizontalFOV else verticalFOV

            val fov = hFOV.toDegrees() to vFOV.toDegrees()
            cachedFOV = fov
            return fov
        } catch (e: Exception) {
            return null
        }
    }

    override fun getZoomedFOV(): Pair<Float, Float>? {
        val fov = getFOV() ?: return null
        val zoom = zoom?.ratio?.coerceAtLeast(0.05f) ?: return fov
        return fov.first / zoom to fov.second / zoom
    }

    override fun setFocusDistancePercentage(distance: Float?) {
        if (distance == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            return
        }

        val min = getCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val max = getCharacteristic(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE) ?: 0f
        val scaled = min + distance * (max - min)
        // Disable auto focus
        setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, scaled)
    }

    override fun setStabilization(opticalStabilization: Boolean, videoStabilization: Boolean) {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            if (videoStabilization) {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            } else {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            }
        )

        setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            if (opticalStabilization) {
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
            } else {
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
            }
        )
    }

    override fun setExposureTime(exposureTime: Duration?) {
        if (exposureTime == null) {
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            return
        }

        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime.toNanos())
    }

    override fun setSensitivity(isoSensitivity: Int?) {
        if (isoSensitivity == null) {
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            return
        }

        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoSensitivity)
    }

    override fun getExposureTimeRange(): Range<Duration>? {
        val range =
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) ?: return null
        return Range(Duration.ofNanos(range.lower), Duration.ofNanos(range.upper))
    }

    override fun getSensitivityRange(): Range<Int>? {
        val range =
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) ?: return null
        return Range(range.lower, range.upper)
    }

    override fun getExposureCompensationRange(): Range<Int>? {
        val range =
            getCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return null
        return Range(range.lower, range.upper)
    }

    override fun isOpticalStabilizationSupported(): Boolean {
        return getCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.any {
            it != CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
        } == true
    }

    override fun isVideoStabilizationSupported(): Boolean {
        return getCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.any {
            it != CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        } == true
    }

    override fun getPreviewRect(cropToView: Boolean): RectF? {
        return tryOrDefault(null) {
            val size = getPreviewSize(cropToView) ?: return@tryOrDefault null
            val layout = layout() ?: return@tryOrDefault null
            // The viewfinder centres the picture.
            val left = (layout.width - size.width) / 2
            val top = (layout.height - size.height) / 2
            RectF(
                left.toFloat(),
                top.toFloat(),
                (left + size.width).toFloat(),
                (top + size.height).toFloat()
            )
        }
    }

    override fun getPreviewSize(cropToView: Boolean): Size? {
        if (cropToView && cachedCroppedPreviewSize != null) {
            return cachedCroppedPreviewSize
        } else if (!cropToView && cachedFullPreviewSize != null) {
            return cachedFullPreviewSize
        }

        val size = tryOrDefault(null) {
            val resolution = preview?.resolutionInfo?.resolution ?: return@tryOrDefault null
            val rotation = preview?.resolutionInfo?.rotationDegrees ?: return@tryOrDefault null
            val layout = layout() ?: return@tryOrDefault null
            if (layout.width == 0 || layout.height == 0) {
                return@tryOrDefault null
            }
            val rotated = if (rotation == 90 || rotation == 270) {
                Size(resolution.height, resolution.width)
            } else {
                resolution
            }
            val viewSize = Size(layout.width, layout.height)
            val ratioBitmap = rotated.width.toFloat() / rotated.height.toFloat()
            val ratioMax = viewSize.width.toFloat() / viewSize.height.toFloat()
            var finalWidth = viewSize.width
            var finalHeight = viewSize.height

            when (layout.scaleType) {
                CameraScaleType.Fill -> {
                    if (cropToView) {
                        viewSize
                    } else {
                        // The size of the preview after it's been scaled to fill the viewfinder
                        if (ratioMax > ratioBitmap) {
                            finalHeight = (viewSize.width.toFloat() / ratioBitmap).toInt()
                        } else {
                            finalWidth = (viewSize.height.toFloat() * ratioBitmap).toInt()
                        }
                        Size(finalWidth, finalHeight)
                    }
                }

                CameraScaleType.Fit -> {
                    if (rotated.width <= viewSize.width && rotated.height <= viewSize.height) {
                        viewSize
                    } else {
                        // Otherwise it gets scaled to fit the viewfinder
                        if (ratioMax > ratioBitmap) {
                            finalWidth = (viewSize.height.toFloat() * ratioBitmap).toInt()
                        } else {
                            finalHeight = (viewSize.width.toFloat() / ratioBitmap).toInt()
                        }
                        Size(finalWidth, finalHeight)
                    }
                }
            }
        }

        if (cropToView) {
            cachedCroppedPreviewSize = size
        } else {
            cachedFullPreviewSize = size
        }

        return size
    }

    override fun getPreviewFOV(cropToView: Boolean): Pair<Float, Float>? {
        val fov = getZoomedFOV() ?: return null

        if (!cropToView) {
            return fov
        }

        val fullPreviewSize = getPreviewSize(false) ?: return null
        val croppedPreviewSize = getPreviewSize(true) ?: return null
        val xFov = if (fullPreviewSize.width > croppedPreviewSize.width) {
            fov.first * (croppedPreviewSize.width / fullPreviewSize.width.toFloat())
        } else {
            fov.first
        }
        val yFov = if (fullPreviewSize.height > croppedPreviewSize.height) {
            fov.second * (croppedPreviewSize.height / fullPreviewSize.height.toFloat())
        } else {
            fov.second
        }
        return Pair(xFov, yFov)
    }

    override fun getIntrinsicCalibration(
        estimateIfUnavailable: Boolean,
        onlyUseEstimated: Boolean
    ): FloatArray? {
        val calibration = if (!onlyUseEstimated) {
            getCharacteristic(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
        } else {
            null
        }

        val shouldEstimate = onlyUseEstimated || (estimateIfUnavailable && calibration == null)

        if (!shouldEstimate) {
            return calibration
        }

        // Estimate the calibration
        val activeArraySize = getActiveArraySize(true) ?: getActiveArraySize(false) ?: return null
        val fullPixelSize = getFullArraySize() ?: return null
        val focalLength = getFocalLength() ?: return null
        val size = getSensorSize() ?: return null

        val w = size.width * activeArraySize.width() / fullPixelSize.width.toFloat()
        val h = size.height * activeArraySize.height() / fullPixelSize.height.toFloat()

        val fx = focalLength * activeArraySize.width() / w
        val fy = focalLength * activeArraySize.height() / h

        val cx = activeArraySize.width() / 2f
        val cy = activeArraySize.height() / 2f

        return floatArrayOf(fx, fy, cx, cy, 0f)
    }

    override fun getActiveArraySize(preCorrection: Boolean): Rect? {
        return if (preCorrection) {
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)
        } else {
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        }
    }

    override fun getFullArraySize(): Size? {
        return getCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    }

    override fun getDistortionCorrection(): FloatArray? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getCharacteristic(CameraCharacteristics.LENS_DISTORTION)
        } else {
            @Suppress("DEPRECATION")
            getCharacteristic(CameraCharacteristics.LENS_RADIAL_DISTORTION)
        }
    }

    override fun getSensorSize(): SizeF? {
        return getCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    }

    override fun getFocalLength(): Float? {
        return getCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCamera2Controller(): Camera2CameraControl? {
        val control = camera?.cameraControl ?: return null
        return Camera2CameraControl.from(control)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCamera2Info(): Camera2CameraInfo? {
        val info = camera?.cameraInfo ?: return null
        return Camera2CameraInfo.from(info)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun <T> getCharacteristic(key: CameraCharacteristics.Key<T>): T? {
        val info = getCamera2Info() ?: return null
        return info.getCameraCharacteristic(key)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun <T : Any> setCaptureRequestOption(key: CaptureRequest.Key<T>, value: T) {
        tryOrNothing {
            val control = getCamera2Controller() ?: return
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(key, value)
                .build()
            control.addCaptureRequestOptions(options)
        }
    }
}
