package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.View
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseFullscreenComposeDialogFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class PhotoImportBottomSheetFragment(
    private val resolution: Size? = null,
    private val useZeroShutterLag: Boolean = false,
    private val onCapture: (uri: Uri?) -> Unit
) : TrailSenseFullscreenComposeDialogFragment() {

    private var isCapturing by mutableStateOf(false)
    private var camera: CameraView? = null
    private var isViewCreated = false
    private var isCameraStarted = false

    private var orientation = DeviceOrientation.Orientation.Portrait

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCapturing = false
    }

    @Composable
    override fun FragmentContent() {
        PhotoImportContent(
            isCapturing = isCapturing,
            onClose = {
                onCapture(null)
                dismiss()
            },
            onCapture = ::takePhoto,
            onCameraReady = {
                camera = it
                if (isViewCreated) {
                    startCamera(it)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true

        val orientationSensor = SensorService(requireContext()).getDeviceOrientationSensor()
        orientation = DeviceOrientation.Orientation.Portrait
        observe(orientationSensor) {
            val newOrientation = orientationSensor.orientation
            if (newOrientation == DeviceOrientation.Orientation.Landscape || newOrientation == DeviceOrientation.Orientation.LandscapeInverse) {
                orientation = newOrientation
            } else if (newOrientation == DeviceOrientation.Orientation.Portrait || newOrientation == DeviceOrientation.Orientation.PortraitInverse) {
                orientation = newOrientation
            }
        }

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            takePhoto()
            true
        }

        dialog?.setOnCancelListener { onCapture(null) }
        camera?.let(::startCamera)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(camera: CameraView) {
        if (isCameraStarted) {
            return
        }

        isCameraStarted = true
        camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        camera.start(
            resolution,
            lifecycleOwner = this,
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
        inBackground {
            val file = FileSubsystem.getInstance(requireContext()).createTemp("jpg")
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
                if (this@PhotoImportBottomSheetFragment.camera == null) {
                    file.delete()
                    return@onMain
                }
                if (!success) {
                    file.delete()
                    onCapture(null)
                } else {
                    onCapture(file.toUri())
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        camera?.stop()
        camera = null
        isCameraStarted = false
        isViewCreated = false
        super.onDestroyView()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        camera?.stop()
    }
}

@Composable
private fun PhotoImportContent(
    isCapturing: Boolean,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onCameraReady: (CameraView) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhotoImportToolbar(
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPreview) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("camera"),
                    factory = { context ->
                        CameraView(context, null).apply {
                            id = R.id.camera
                            setFlipEnabled(true)
                            onCameraReady(this)
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("camera")
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.lg),
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
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
            .padding(start = TrailSenseSpacing.xl, end = TrailSenseSpacing.sm)
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
        IconButton(onClick = onClose) {
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
