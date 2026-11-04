package com.kylecorry.trail_sense.shared.views

import android.graphics.drawable.Drawable
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.ColorInt
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.bitmaps.BitmapUtils.toBitmap
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.R
import java.io.File
import java.time.Duration
import kotlin.math.roundToInt

class CameraState(val context: Context) {

    internal val preview = PreviewView(context).apply {
        id = R.id.camera_preview
        // A TextureView, not a SurfaceView: it composites with Compose, so the preview shows inside rounded,
        // clipped windows (a SurfaceView drew behind the window's background and the picture stayed blank).
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    var camera: ICamera? = null
        private set

    private var lastFov: Pair<Float, Float>? = null

    val fov: Pair<Float, Float>
        get() {
            val defaultFOV = 45f
            val fieldOfView =
                camera?.getZoomedFOV() ?: lastFov ?: (defaultFOV to defaultFOV * 4f / 3f)
            lastFov = fieldOfView
            return fieldOfView
        }

    val width: Int
        get() = preview.width

    var defaultZoomRatio: Float = 1f
    var minZoomRatio: Float? = null
    var keepScreenOn: Boolean
        get() = preview.keepScreenOn
        set(value) {
            preview.keepScreenOn = value
        }

    var clipToOutline: Boolean
        get() = preview.clipToOutline
        set(value) {
            preview.clipToOutline = value
        }

    var isVisible by mutableStateOf(true)

    var isInvisible: Boolean
        get() = invisibleState
        set(value) {
            invisibleState = value
            if (!value) {
                isVisible = true
            }
        }

    var passThroughTouchEvents = false

    val previewImage: Bitmap?
        get() = try {
            preview.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    var isStarted = false
        private set

    internal var showTorchState by mutableStateOf(true)
        private set
    internal var flipEnabledState by mutableStateOf(false)
        private set
    internal var torchOn by mutableStateOf(false)
        private set
    internal var zoomValue by mutableFloatStateOf(0f)
        private set
    internal var invisibleState by mutableStateOf(false)
        private set
    internal var previewBitmap by mutableStateOf<Bitmap?>(null)
        private set

    private var backgroundDrawable: Drawable? = null
    private var backgroundColor: Int? = null
    private var zoomListener: ((Float) -> Unit)? = null
    private var clickListener: View.OnClickListener? = null
    private var imageListener: ((Bitmap) -> Unit)? = null
    private var captureListener: ((Bitmap) -> Unit)? = null
    private var isCapturing = false
    private var exposureCompensation = 0f
    private var exposureTime: Duration? = null
    private var sensitivity: Int? = null
    private var focus: Float? = null
    private var hasPendingChanges = false
    private var hasSetInitialZoom = false
    private var composeLifecycleOwner: LifecycleOwner? = null
    private val startLock = Any()

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoomValue != -1f) {
                val remainingZoom = 1 - zoomValue
                val newZoom = (zoomValue + remainingZoom / 2).coerceIn(0f, 1f)
                zoomListener?.invoke(newZoom)
                setZoom(newZoom)
                return true
            }
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            clickListener?.onClick(preview)
            return clickListener != null || super.onSingleTapConfirmed(e)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (zoomValue != -1f) {
                val newZoom = (zoomValue - 1 + detector.scaleFactor).coerceIn(0f, 1f)
                zoomListener?.invoke(newZoom)
                setZoom(newZoom)
                return true
            }
            return false
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)

    init {
        preview.setOnTouchListener { _, event ->
            onTouchEvent(event)
        }
    }

    fun start(
        resolution: Size? = null,
        lifecycleOwner: LifecycleOwner? = null,
        captureSettings: ImageCaptureSettings? = null,
        readFrames: Boolean = true,
        shouldStabilizePreview: Boolean = true,
        preferBackCamera: Boolean = true,
        onImage: ((Bitmap) -> Unit)? = null
    ) {
        val owner = lifecycleOwner ?: composeLifecycleOwner ?: return
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }

        if (!Camera.isAvailable(context)) {
            Alerts.toast(context, context.getString(R.string.camera_unavailable))
            return
        }

        val useBackCamera = preferBackCamera && Camera.hasBackCamera(context)

        if (!useBackCamera && !Camera.hasFrontCamera(context)) {
            Alerts.toast(context, context.getString(R.string.camera_unavailable))
            return
        }

        synchronized(startLock) {
            if (isStarted) {
                return
            }
            isStarted = true
        }

        keepScreenOn = true
        camera?.stop(this::onCameraUpdate)
        imageListener = onImage
        camera = Camera(
            context,
            owner,
            previewView = preview,
            analyze = true,
            targetResolution = resolution,
            captureSettings = captureSettings,
            isBackCamera = useBackCamera,
            shouldStabilizePreview = shouldStabilizePreview
        )
        hasPendingChanges = true
        camera?.start(this::onCameraUpdate)
    }

    fun stop() {
        camera?.stop(this::onCameraUpdate)
        camera = null
        keepScreenOn = false
        imageListener = null
        previewBitmap = null
        synchronized(startLock) {
            isStarted = false
        }
    }

    suspend fun capture(file: File): Boolean {
        synchronized(this) {
            if (isCapturing) {
                return true
            }
            isCapturing = true
        }
        val success = camera?.takePhoto(file) ?: false
        synchronized(this) {
            isCapturing = false
        }
        return success
    }

    fun setTorch(isOn: Boolean) {
        torchOn = isOn
        camera?.setTorch(torchOn)
        hasPendingChanges = true
    }

    fun setShowTorch(shouldShow: Boolean) {
        showTorchState = shouldShow
    }

    fun setScaleType(type: PreviewView.ScaleType) {
        preview.scaleType = type
    }

    fun setPreviewBackgroundColor(@ColorInt color: Int) {
        backgroundColor = color
        preview.setBackgroundColor(color)
    }

    fun setFocus(value: Float?) {
        focus = value
        camera?.setFocusDistancePercentage(value)
        hasPendingChanges = true
    }

    fun setZoom(zoom: Float) {
        hasSetInitialZoom = true
        zoomValue = zoom
        val state = camera?.zoom
        val min = minZoomRatio ?: state?.ratioRange?.start ?: 1f
        val max = state?.ratioRange?.end ?: 2f
        camera?.setZoomRatio(Interpolation.map(zoom, 0f, 1f, min, max))
        hasPendingChanges = true
    }

    fun setZoomFromUser(zoom: Float) {
        zoomListener?.invoke(zoom)
        setZoom(zoom)
    }

    fun setZoomRatio(ratio: Float) {
        val state = camera?.zoom
        val min = minZoomRatio ?: state?.ratioRange?.start ?: 1f
        val max = state?.ratioRange?.end ?: 2f
        val pct = Interpolation.norm(ratio, min, max)
        setZoom(pct)
    }

    fun setOnZoomChangeListener(listener: ((zoom: Float) -> Unit)?) {
        zoomListener = listener
    }

    fun setOnClickListener(listener: View.OnClickListener?) {
        clickListener = listener
    }

    fun setBackground(drawable: Drawable?) {
        backgroundDrawable = drawable
        preview.background = drawable
    }

    fun setFlipEnabled(enabled: Boolean) {
        flipEnabledState = enabled
    }

    fun dispatchTouchEvent(event: MotionEvent) {
        onTouchEvent(event)
    }

    internal fun applyViewAttributes() {
        backgroundDrawable?.let { preview.background = it }
        backgroundColor?.let(preview::setBackgroundColor)
        preview.clipToOutline = clipToOutline
    }

    internal fun setLifecycleOwner(owner: LifecycleOwner) {
        composeLifecycleOwner = owner
    }

    fun flipCamera() {
        camera?.flipCamera()
    }

    fun setExposureCompensation(value: Float) {
        exposureCompensation = value

        val range = camera?.getExposureCompensationRange() ?: Range(0, 0)
        val mapped = if (range.start == range.end) {
            range.start
        } else {
            Interpolation.map(value, -1f, 1f, range.start.toFloat(), range.end.toFloat())
                .roundToInt()
        }

        camera?.setExposure(mapped)
        hasPendingChanges = true
    }

    fun setManualExposure(exposureTime: Duration?, sensitivity: Int?) {
        this.exposureTime = exposureTime
        this.sensitivity = sensitivity
        camera?.setExposureTime(exposureTime)
        camera?.setSensitivity(sensitivity)
        hasPendingChanges = true
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        if (!hasSetInitialZoom) {
            setZoomRatio(defaultZoomRatio)
        }
        if (hasPendingChanges) {
            setZoom(zoomValue)
            setExposureCompensation(exposureCompensation)
            setFocus(focus)
            setManualExposure(exposureTime, sensitivity)
            camera?.setTorch(torchOn)
            hasPendingChanges = false
        }
        if (captureListener == null && imageListener == null && invisibleState) {
            camera?.image?.close()
            return true
        }
        try {
            val rotation = camera?.image?.imageInfo?.rotationDegrees?.toFloat() ?: 0f
            val bitmap = camera?.image?.image?.toBitmap(rotation)
            bitmap?.let {
                previewBitmap = it
                captureListener?.invoke(it)
                imageListener?.invoke(it)

                synchronized(this) {
                    captureListener = null
                }
            }
        } catch (e: Exception) {
            // Do nothing
        } finally {
            camera?.image?.close()
        }
        return true
    }

    private fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return !passThroughTouchEvents
    }
}

@Composable
fun CameraPreview(
    state: CameraState,
    modifier: Modifier = Modifier,
    showZoomSlider: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Box(
        modifier = modifier.alpha(
            if (state.isVisible && !state.invisibleState) {
                1f
            } else {
                0f
            }
        ).pointerInteropFilter {
            state.dispatchTouchEvent(it)
            !state.passThroughTouchEvents
        }
    ) {
        state.applyViewAttributes()
        // The real CameraX preview, on screen: without its surface in the window the camera opens but never
        // delivers a frame (the preview stayed blank). A TextureView, so it clips to rounded windows.
        androidx.compose.ui.viewinterop.AndroidView(
            factory = {
                (state.preview.parent as? android.view.ViewGroup)?.removeView(state.preview)
                state.preview
            },
            modifier = Modifier.fillMaxSize()
        )
        if (state.flipEnabledState) {
            IconButton(
                onClick = state::flipCamera,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = TrailIcons.CameraFlip,
                    contentDescription = null
                )
            }
        }
        if (state.showTorchState) {
            IconButton(
                onClick = { state.setTorch(!state.torchOn) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(
                        if (state.torchOn) {
                            TrailIcons.TorchOn
                        } else {
                            TrailIcons.TorchOff
                        }
                    ),
                    contentDescription = null
                )
            }
        }
        if (showZoomSlider) {
            Slider(
                value = state.zoomValue.coerceIn(0f, 1f),
                onValueChange = {
                    state.setZoomFromUser(it)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
    state.setLifecycleOwner(lifecycleOwner)
}
