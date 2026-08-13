package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PhotoImportDialog(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope,
    private val resolution: Size? = null,
    private val useZeroShutterLag: Boolean = false,
    private val onCapture: (uri: Uri?) -> Unit
) {

    private var isCapturing by mutableStateOf(false)
    private var camera: CameraState? = null
    private var isCameraStarted = false
    private var isVisible by mutableStateOf(false)

    private var orientation = DeviceOrientation.Orientation.Portrait
    private val orientationSensor by lazy {
        SensorService(context).getDeviceOrientationSensor()
    }
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> startOrientationSensor()
            Lifecycle.Event.ON_PAUSE -> stopOrientationSensor()
            Lifecycle.Event.ON_DESTROY -> stopOrientationSensor()
            else -> {}
        }
    }

    init {
        initialize()
    }

    fun show() {
        isCapturing = false
        isVisible = true
    }

    @Composable
    fun Content() {
        if (!isVisible) {
            return
        }

        Dialog(
            onDismissRequest = { close(cancelled = true) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            PhotoImportContent(
                isCapturing = isCapturing,
                onClose = { close(cancelled = true) },
                onCapture = ::takePhoto,
                onCameraReady = {
                    camera = it
                    startCamera(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        if (it.key != Key.VolumeDown && it.key != Key.VolumeUp) {
                            return@onPreviewKeyEvent false
                        }
                        takePhoto()
                        true
                    }
                    .windowInsetsPadding(WindowInsets.systemBars)
            )
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun initialize() {
        orientation = DeviceOrientation.Orientation.Portrait
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startOrientationSensor()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(camera: CameraState) {
        if (isCameraStarted) {
            return
        }

        isCameraStarted = true
        camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        camera.start(
            resolution,
            lifecycleOwner = lifecycleOwner,
            readFrames = false,
            captureSettings = ImageCaptureSettings(
                captureMode = if (useZeroShutterLag) {
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                } else if (resolution == null) {
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                } else {
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
            )
        )
    }

    private fun takePhoto() {
        val camera = camera ?: return
        if (isCapturing) {
            return
        }
        isCapturing = true
        coroutineScope.launch {
            val file = FileSubsystem.getInstance(context).createTemp("jpg")
            val success = onIO {
                val saved = camera.capture(file)

                tryOrLog {
                    if (orientation != DeviceOrientation.Orientation.Portrait) {
                        val exif = ExifInterface(file)
                        exif.rotate(
                            when (orientation) {
                                DeviceOrientation.Orientation.PortraitInverse -> 180
                                DeviceOrientation.Orientation.Landscape -> 270
                                DeviceOrientation.Orientation.LandscapeInverse -> 90
                                else -> 0
                            }
                        )
                        exif.saveAttributes()
                    }
                }

                saved
            }
            onMain {
                if (this@PhotoImportDialog.camera == null) {
                    file.delete()
                    return@onMain
                }
                if (!success) {
                    file.delete()
                    onCapture(null)
                } else {
                    onCapture(file.toUri())
                }
                close(cancelled = false)
            }
        }
    }

    fun dismiss() {
        close(cancelled = false)
    }

    private fun close(cancelled: Boolean) {
        if (!isVisible && camera == null) {
            return
        }
        if (cancelled) {
            onCapture(null)
        }
        isVisible = false
        stopCamera()
        stopOrientationSensor()
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
    }

    private fun stopCamera() {
        camera?.stop()
        camera = null
        isCameraStarted = false
    }

    private fun startOrientationSensor() {
        orientationSensor.start(this::onOrientationReading)
    }

    private fun stopOrientationSensor() {
        orientationSensor.stop(this::onOrientationReading)
    }

    private fun onOrientationReading(): Boolean {
        val newOrientation = orientationSensor.orientation
        if (newOrientation == DeviceOrientation.Orientation.Landscape ||
            newOrientation == DeviceOrientation.Orientation.LandscapeInverse
        ) {
            orientation = newOrientation
        } else if (newOrientation == DeviceOrientation.Orientation.Portrait ||
            newOrientation == DeviceOrientation.Orientation.PortraitInverse
        ) {
            orientation = newOrientation
        }
        return true
    }
}

@Composable
private fun PhotoImportContent(
    isCapturing: Boolean,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onCameraReady: (CameraState) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    val cameraDescription = stringResource(R.string.camera)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhotoImportToolbar(
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPreview) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val cameraState = remember {
                    CameraState(context).apply {
                        setFlipEnabled(true)
                    }
                }
                LaunchedEffect(cameraState) {
                    onCameraReady(cameraState)
                }
                CameraPreview(
                    state = cameraState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = TrailSenseSpacing.screen)
                        .clip(MaterialTheme.shapes.large)
                        .semantics { contentDescription = cameraDescription }
                        .testTag("camera")
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = TrailSenseSpacing.screen)
                        .clip(MaterialTheme.shapes.large)
                        .semantics { contentDescription = cameraDescription }
                        .testTag("camera")
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.screen),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("loading_indicator")
                    )
                } else {
                    CaptureButton(
                        onClick = onCapture,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("capture_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDescription = stringResource(R.string.camera_capture)
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun PhotoImportToolbar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(start = TrailSenseSpacing.screen, end = TrailSenseSpacing.sm)
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("tool_title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.take_photo),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(android.R.string.cancel)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoImportContentPreview() {
    TrailSenseTheme {
        PhotoImportContent(
            isCapturing = false,
            onClose = {},
            onCapture = {},
            onCameraReady = {}
        )
    }
}
