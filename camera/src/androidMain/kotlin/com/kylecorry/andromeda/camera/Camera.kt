package com.kylecorry.andromeda.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Range
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds


internal class Camera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val isBackCamera: Boolean = true,
    private val previewState: CameraPreviewState? = null,
    private val analyze: Boolean = true,
    private val targetResolution: Size? = null,
    private val useYUV: Boolean = false,
    private val captureSettings: ImageCaptureSettings? = null,
    private val shouldStabilizePreview: Boolean = true,
    @param:AspectRatio.Ratio
    private val targetAspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    private val onAnalysisFrame: ((ImageProxy) -> Unit)? = null,
    private val onAnalysisError: (Throwable) -> Unit = {},
) : SharedSensor(), ICamera {

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

    internal val exposureCompensation: CameraExposure?
        get() {
            val state = camera?.cameraInfo?.exposureState ?: return null
            if (!state.isExposureCompensationSupported) return null
            val step = state.exposureCompensationStep.toFloat()
            if (step <= 0f) return null
            val range = state.exposureCompensationRange
            return CameraExposure(state.exposureCompensationIndex * step, range.lower * step, range.upper * step, step)
        }

    internal fun setExposureCompensation(value: Float) {
        val exposure = checkNotNull(exposureCompensation) { "Exposure compensation is unavailable" }
        require(value.isFinite() && value in exposure.minimum..exposure.maximum)
        val step = checkNotNull(exposure.step)
        val index = (value / step).roundToInt()
        checkNotNull(camera).cameraControl.setExposureCompensationIndex(index)
    }

    internal val manualExposureRange: ManualExposureRange?
        get() {
            val modes = getCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: return null
            val capabilities = getCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return null
            if (CameraMetadata.CONTROL_AE_MODE_OFF !in modes ||
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR !in capabilities) return null
            val shutter = getCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) ?: return null
            val sensitivity = getCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) ?: return null
            return ManualExposureRange(shutter.lower.nanoseconds..shutter.upper.nanoseconds, sensitivity.lower..sensitivity.upper)
        }

    @OptIn(ExperimentalCamera2Interop::class)
    internal fun setSharedManualExposure(duration: kotlin.time.Duration?, iso: Int?) {
        require((duration == null) == (iso == null)) { "Provide both duration and ISO, or neither" }
        val control = checkNotNull(getCamera2Controller()) { "Camera is unavailable" }
        val options = CaptureRequestOptions.Builder()
        if (duration == null) {
            options.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
        } else {
            val supported = checkNotNull(manualExposureRange) { "Manual exposure is unavailable" }
            val sensitivity = checkNotNull(iso)
            require(duration.isFinite() && duration in supported.duration && sensitivity in supported.iso)
            options.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            options.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, duration.inWholeNanoseconds)
            options.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, sensitivity)
        }
        control.addCaptureRequestOptions(options.build())
    }

    internal val manualFocusSupported: Boolean
        get() = (getCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f) > 0f &&
            getCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.contains(CameraMetadata.CONTROL_AF_MODE_OFF) == true

    internal fun setSharedFocus(position: Float?) {
        if (position != null) {
            require(position.isFinite() && position in 0f..1f)
            check(manualFocusSupported) { "Manual focus is unavailable" }
            setFocusDistancePercentage(position)
            return
        }
        val modes = getCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            ?: error("Focus control is unavailable")
        val mode = when {
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE in modes -> CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            CameraMetadata.CONTROL_AF_MODE_AUTO in modes -> CameraMetadata.CONTROL_AF_MODE_AUTO
            else -> error("Autofocus is unavailable")
        }
        setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, mode)
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

    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
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

                    // TODO: This might not be needed now that the method is exposed
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
                setOutputImageFormat(if (useYUV) OUTPUT_IMAGE_FORMAT_YUV_420_888 else OUTPUT_IMAGE_FORMAT_RGBA_8888)
            }.build()

            val analyzerExecutor = if (onAnalysisFrame != null) Dispatchers.Default.asExecutor()
                else ContextCompat.getMainExecutor(context)
            imageAnalysis.setAnalyzer(analyzerExecutor) { image ->
                if (onAnalysisFrame != null) {
                    try { onAnalysisFrame.invoke(image) }
                    catch (error: Exception) { onAnalysisError(error) }
                    finally { image.close() }
                } else {
                    _image = image
                    _hasValidReading = true
                    setEventTimeToNow()
                    notifyListeners()
                }
            }

            if (captureSettings != null) {
                val builder = ImageCapture.Builder()
                    .setFlashMode(captureSettings.flashMode)
                    .setCaptureMode(captureSettings.captureMode)

                captureSettings.quality?.let {
                    builder.setJpegQuality(it)
                }

                builder.setResolutionSelector(resolutionSelector)

                captureSettings.rotation?.let {
                    builder.setTargetRotation(it)
                }

                imageCapture = builder.build()
            } else {
                imageCapture = null
            }

            if (cameraSelector == null) {
                cameraSelector =
                    if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            }

            preview?.setSurfaceProvider { request -> previewState?.surfaceRequest = request }

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA,
                *listOfNotNull(
                    if (previewState != null) preview else null,
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

    fun setFlashMode(mode: Int) { imageCapture?.flashMode = mode }

    override fun flipCamera() {
        cameraProvider?.let {
            cameraSelector?.let {
                cameraSelector = if (cameraSelector === CameraSelector.DEFAULT_FRONT_CAMERA)
                    CameraSelector.DEFAULT_BACK_CAMERA
                else
                    CameraSelector.DEFAULT_FRONT_CAMERA

                lifecycleOwner.lifecycleScope.launch {
                    stopImpl()
                    startImpl()
                }
            }
        }
    }

    override fun stopImpl() {
        cameraProvider?.unbindAll()
        previewState?.surfaceRequest = null
        cameraProvider = null
        cameraProviderFuture?.cancel(true)
        cameraProviderFuture = null
        cachedFOV = null
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
    }

    private fun sharedMeteringAction(x: Float, y: Float): FocusMeteringAction {
        val size = checkNotNull(previewState?.size) { "Camera preview is unavailable" }
        check(size.width > 0 && size.height > 0) { "Camera preview is not laid out" }
        val rect = checkNotNull(getPreviewRect(false)) { "Camera preview is unavailable" }
        val point = SurfaceOrientedMeteringPointFactory(rect.width(), rect.height())
            .createPoint(x * size.width - rect.left, y * size.height - rect.top)
        return FocusMeteringAction.Builder(point).build()
    }

    internal val sharedMeteringSupported: Boolean
        get() = try {
            camera?.cameraInfo?.isFocusMeteringSupported(sharedMeteringAction(0.5f, 0.5f)) == true
        } catch (_: IllegalStateException) { false }

    internal fun focusAt(x: Float, y: Float) {
        require(x.isFinite() && x in 0f..1f && y.isFinite() && y in 0f..1f)
        val current = checkNotNull(camera) { "Camera is unavailable" }
        val action = sharedMeteringAction(x, y)
        check(current.cameraInfo.isFocusMeteringSupported(action)) { "Focus and metering are unavailable" }
        current.cameraControl.startFocusAndMetering(action)
    }

    override fun stopFocusAndMetering() {
        tryOrNothing {
            camera?.cameraControl?.cancelFocusAndMetering()
        }
    }

    override fun startFocusAndMetering(point: PixelCoordinate) {
        val rect = getPreviewRect(false) ?: return
        if (rect.width() <= 0f || rect.height() <= 0f) return
        val meteringPoint = SurfaceOrientedMeteringPointFactory(rect.width(), rect.height())
            .createPoint(point.x - rect.left, point.y - rect.top)
        val action = FocusMeteringAction.Builder(meteringPoint).build()

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

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageCapturedCallback() {
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
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                cont.resume(true)
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resume(false)
            }
        })
    }

    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    fun getCameraId(): String? {
        try {
            val info = getCamera2Info() ?: return getDefaultCameraId(context, isBackCamera)
            return info.cameraId
        } catch (e: Exception) {
            return getDefaultCameraId(context, isBackCamera)
        }
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
            // TODO: The approach outlined in this stackoverflow post also handles cropping
            // https://stackoverflow.com/questions/39965408/what-is-the-android-camera2-api-equivalent-of-camera-parameters-gethorizontalvie
            val horizontalFOV = 2 * atan(w / (2 * focalLength))
            val verticalFOV = 2 * atan(h / (2 * focalLength))

            val isFlipped = sensorOrientation == 90 || sensorOrientation == 270

            val hFOV = if (isFlipped) {
                verticalFOV
            } else {
                horizontalFOV
            }

            val vFOV = if (isFlipped) {
                horizontalFOV
            } else {
                verticalFOV
            }

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
            if (videoStabilization) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        )

        setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            if (opticalStabilization) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
        )
    }

    override fun setExposureTime(exposureTime: Duration?) {
        if (exposureTime == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            return
        }

        val nanos = exposureTime.toNanos()
        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, nanos)
    }

    override fun setSensitivity(isoSensitivity: Int?) {
        if (isoSensitivity == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
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
        return getCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
    }

    override fun isVideoStabilizationSupported(): Boolean {
        return getCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            ?.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
    }

    override fun getPreviewRect(cropToView: Boolean): RectF? {
        val bounds = previewState?.size ?: return null
        val previewSize = getPreviewSize(cropToView) ?: return null
        val left = (bounds.width - previewSize.width) / 2f
        val top = (bounds.height - previewSize.height) / 2f
        return RectF(left, top, left + previewSize.width, top + previewSize.height)
    }

    override fun getPreviewSize(cropToView: Boolean): Size? {
        val bounds = previewState?.size ?: return null
        if (bounds.width <= 0 || bounds.height <= 0) return null
        val info = preview?.resolutionInfo ?: return null
        val resolution = info.resolution
        val rotated = if (info.rotationDegrees == 90 || info.rotationDegrees == 270) {
            Size(resolution.height, resolution.width)
        } else resolution
        if (rotated.width <= 0 || rotated.height <= 0) return null
        val widthScale = bounds.width.toFloat() / rotated.width
        val heightScale = bounds.height.toFloat() / rotated.height
        val scale = if (previewState.fill) maxOf(widthScale, heightScale) else minOf(widthScale, heightScale)
        return if (cropToView && previewState.fill) Size(bounds.width, bounds.height)
        else Size((rotated.width * scale).toInt(), (rotated.height * scale).toInt())
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
        tryOrDefault(false) {
            val control = getCamera2Controller() ?: return
            val newCaptureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(key, value)
                .build()
            control.addCaptureRequestOptions(newCaptureRequestOptions)
        }
    }

    companion object {
        @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
        fun isAvailable(context: Context): Boolean {
            if (!Permissions.isCameraEnabled(context)) {
                return false
            }
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }

        fun hasBackCamera(context: Context): Boolean {
            val cameras = getCameras(context)
            return cameras.any {
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
            }
        }

        fun hasFrontCamera(context: Context): Boolean {
            val cameras = getCameras(context)
            return cameras.any {
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
            }
        }

        private fun getDefaultCameraId(context: Context, isBackCamera: Boolean): String? {
            return tryOrDefault(null) {
                val manager =
                    context.getSystemService<CameraManager>() ?: return@tryOrDefault null
                manager.cameraIdList.firstOrNull {
                    val characteristics = manager.getCameraCharacteristics(it)
                    val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)
                    orientation == if (isBackCamera) CameraMetadata.LENS_FACING_BACK else CameraMetadata.LENS_FACING_FRONT
                }
            }
        }

        private fun getCameras(context: Context): List<CameraCharacteristics> {
            return tryOrDefault(emptyList()) {
                val manager =
                    context.getSystemService<CameraManager>() ?: return@tryOrDefault emptyList()
                manager.cameraIdList.map { manager.getCameraCharacteristics(it) }
            }
        }
    }

}
