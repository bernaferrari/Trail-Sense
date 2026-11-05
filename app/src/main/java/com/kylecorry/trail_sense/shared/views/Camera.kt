package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.bitmaps.BitmapUtils.toBitmap
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.camera.CameraScaleType
import com.kylecorry.trail_sense.shared.camera.ViewfinderCamera
import com.kylecorry.trail_sense.shared.camera.ViewfinderLayout
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.icons.trail.CameraFlip
import com.kylecorry.trail_sense.shared.icons.trail.TorchOff
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The camera behind a [CameraPreview]: a CameraX camera bound to a lifecycle, whose picture is drawn by CameraX's
 * Compose viewfinder. Holds zoom, torch, focus and exposure, delivers analysis frames, and captures photos.
 */
class CameraState(val context: Context) {

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

    /** The viewfinder's width on screen, in pixels (0 until it is laid out). */
    val width: Int
        get() = viewfinderWidth

    var defaultZoomRatio: Float = 1f
    var minZoomRatio: Float? = null
    var keepScreenOn by mutableStateOf(false)

    var isVisible by mutableStateOf(true)

    var isInvisible: Boolean
        get() = invisibleState
        set(value) {
            invisibleState = value
            if (!value) {
                isVisible = true
            }
        }

    /**
     * Has no effect: the viewfinder always clips to the shape its [CameraPreview] modifier gives it. Kept until the
     * beacon list's QR import stops setting it.
     */
    @Deprecated("The viewfinder clips to its modifier's shape; clip the CameraPreview instead.")
    var clipToOutline: Boolean = true

    /** When true, touches on the preview zoom the camera but are left for whatever is under or around it. */
    var passThroughTouchEvents = false

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
    internal var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
        private set
    internal var scaleTypeState by mutableStateOf(CameraScaleType.Fill)
        private set
    internal var backgroundColor by mutableStateOf(Color.Black)
        private set
    internal val coordinateTransformer = MutableCoordinateTransformer()

    @Volatile
    private var viewfinderWidth = 0

    @Volatile
    private var viewfinderHeight = 0

    private var zoomListener: ((Float) -> Unit)? = null
    private var clickListener: (() -> Unit)? = null
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
        camera = ViewfinderCamera(
            context,
            owner,
            isBackCamera = useBackCamera,
            onSurfaceRequest = { surfaceRequest = it },
            layout = { ViewfinderLayout(viewfinderWidth, viewfinderHeight, scaleTypeState) },
            toSurfacePoint = ::toSurfacePoint,
            analyze = true,
            targetResolution = resolution,
            captureSettings = captureSettings,
            shouldStabilizePreview = shouldStabilizePreview
        )
        hasPendingChanges = true
        camera?.start(this::onCameraUpdate)
    }

    fun stop() {
        camera?.stop(this::onCameraUpdate)
        camera = null
        surfaceRequest = null
        keepScreenOn = false
        imageListener = null
        synchronized(this) {
            captureListener = null
        }
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

    /** The next camera frame, upright, or null if the camera stops first. */
    suspend fun nextFrame(): Bitmap? {
        if (camera == null) {
            return null
        }
        return suspendCancellableCoroutine { cont ->
            synchronized(this) {
                captureListener = { if (cont.isActive) cont.resume(it) }
            }
            cont.invokeOnCancellation {
                synchronized(this) {
                    captureListener = null
                }
            }
        }
    }

    fun setTorch(isOn: Boolean) {
        torchOn = isOn
        camera?.setTorch(torchOn)
        hasPendingChanges = true
    }

    fun setShowTorch(shouldShow: Boolean) {
        showTorchState = shouldShow
    }

    fun setScaleType(type: CameraScaleType) {
        scaleTypeState = type
    }

    fun setPreviewBackgroundColor(color: Color) {
        backgroundColor = color
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

    fun setOnClickListener(listener: (() -> Unit)?) {
        clickListener = listener
    }

    fun setFlipEnabled(enabled: Boolean) {
        flipEnabledState = enabled
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

    /** A pinch: the zoom grows by how much the fingers spread ([scaleFactor] above 1) or shrinks. */
    fun zoomBy(scaleFactor: Float) {
        val newZoom = (zoomValue - 1 + scaleFactor).coerceIn(0f, 1f)
        setZoomFromUser(newZoom)
    }

    /** A double tap: zoom halfway to the maximum. */
    fun zoomIn() {
        val remainingZoom = 1 - zoomValue
        setZoomFromUser((zoomValue + remainingZoom / 2).coerceIn(0f, 1f))
    }

    /** A single tap on the preview. */
    fun click() {
        clickListener?.invoke()
    }

    internal fun setLifecycleOwner(owner: LifecycleOwner) {
        composeLifecycleOwner = owner
    }

    internal fun setViewfinderSize(width: Int, height: Int) {
        viewfinderWidth = width
        viewfinderHeight = height
    }

    private fun toSurfacePoint(point: PixelCoordinate): PixelCoordinate? {
        if (surfaceRequest == null) {
            return null
        }
        val surface = with(coordinateTransformer) { Offset(point.x, point.y).transform() }
        return PixelCoordinate(surface.x, surface.y)
    }

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
        // Frames only become bitmaps when someone is waiting for one.
        if (captureListener == null && imageListener == null) {
            camera?.image?.close()
            return true
        }
        try {
            val rotation = camera?.image?.imageInfo?.rotationDegrees?.toFloat() ?: 0f
            val bitmap = camera?.image?.image?.toBitmap(rotation)
            bitmap?.let {
                val capture = synchronized(this) {
                    captureListener.also { captureListener = null }
                }
                capture?.invoke(it)
                imageListener?.invoke(it)
            }
        } catch (e: Exception) {
            // Do nothing
        } finally {
            camera?.image?.close()
        }
        return true
    }
}

/**
 * The camera's picture, drawn by CameraX's Compose viewfinder, with the torch and flip buttons and a zoom slider
 * over it. Pinch to zoom, double tap to zoom in, tap for the camera's click listener.
 */
@Composable
fun CameraPreview(
    state: CameraState,
    modifier: Modifier = Modifier,
    showZoomSlider: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    state.setLifecycleOwner(lifecycleOwner)
    Box(
        modifier = modifier
            .then(if (state.keepScreenOn) Modifier.keepScreenOn() else Modifier)
            .alpha(if (state.isVisible && !state.invisibleState) 1f else 0f)
            .onSizeChanged { state.setViewfinderSize(it.width, it.height) }
            .cameraGestures(camera = { state }, consume = { !state.passThroughTouchEvents }) {
                state.click()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(state.backgroundColor)
        ) {
            state.surfaceRequest?.let { request ->
                CameraXViewfinder(
                    surfaceRequest = request,
                    // Embedded (a texture), so the picture clips to rounded windows and fades with its parent.
                    implementationMode = ImplementationMode.EMBEDDED,
                    coordinateTransformer = state.coordinateTransformer,
                    alignment = Alignment.Center,
                    contentScale = when (state.scaleTypeState) {
                        CameraScaleType.Fill -> ContentScale.Crop
                        CameraScaleType.Fit -> ContentScale.Fit
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
                        if (state.torchOn) TrailIcons.TorchOn else TrailIcons.TorchOff
                    ),
                    contentDescription = null
                )
            }
        }
        if (showZoomSlider) {
            Slider(
                value = state.zoomValue.coerceIn(0f, 1f),
                onValueChange = { state.setZoomFromUser(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * The camera's touch gestures, for the preview itself or for an overlay drawn over it: pinch to zoom, double tap to
 * zoom in, and a single tap ([onTap], fired once a double tap is ruled out). With [consume] false the touches are
 * watched but left for the content underneath or around.
 */
fun Modifier.cameraGestures(
    camera: () -> CameraState?,
    consume: () -> Boolean = { true },
    enabled: () -> Boolean = { true },
    onTap: (Offset) -> Unit = {}
): Modifier = this
    .pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2 && enabled()) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        camera()?.zoomBy(zoom)
                    }
                    if (consume()) {
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!enabled()) {
                return@awaitEachGesture
            }
            val up = awaitTap(down) ?: return@awaitEachGesture
            if (consume()) {
                up.consume()
            }
            val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                awaitFirstDown()
            }
            if (secondDown == null) {
                onTap(up.position)
                return@awaitEachGesture
            }
            val secondUp = awaitTap(secondDown) ?: return@awaitEachGesture
            if (consume()) {
                secondUp.consume()
            }
            camera()?.zoomIn()
        }
    }

/** Waits for [down] to lift as a tap: one finger, within the touch slop and before a long press. */
private suspend fun AwaitPointerEventScope.awaitTap(down: PointerInputChange): PointerInputChange? {
    return withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        var up: PointerInputChange? = null
        var isTap = true
        while (isTap && up == null) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id }
            val moved = change?.let { it.position - down.position }
            isTap = change != null &&
                    event.changes.count { it.pressed } <= 1 &&
                    moved != null &&
                    abs(moved.x) <= viewConfiguration.touchSlop &&
                    abs(moved.y) <= viewConfiguration.touchSlop
            if (isTap && change?.pressed == false) {
                isTap = !change.isConsumed
                up = change
            }
        }
        if (isTap) up else null
    }
}
