package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Size
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
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
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class PhotoImportDialog(
    private val fragment: Fragment,
    private val resolution: Size? = null,
    private val useZeroShutterLag: Boolean = false,
    private val onCapture: (uri: Uri?) -> Unit
) {

    private var isCapturing by mutableStateOf(false)
    private var camera: CameraState? = null
    private var isCameraStarted = false
    private var dialog: Dialog? = null

    private var orientation = DeviceOrientation.Orientation.Portrait

    fun show(): Dialog {
        isCapturing = false
        val context = fragment.requireContext()
        val newDialog = Dialog(context)
        dialog = newDialog
        newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newDialog.setContentView(
            TrailSenseComposeRootView(
                context,
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            ) {
                PhotoImportContent(
                    isCapturing = isCapturing,
                    onClose = {
                        onCapture(null)
                        dismiss()
                    },
                    onCapture = ::takePhoto,
                    onCameraReady = {
                        camera = it
                        startCamera(it)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                )
            }
        )
        newDialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        initializeDialog(newDialog)
        newDialog.show()
        newDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        return newDialog
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initializeDialog(dialog: Dialog) {
        val orientationSensor = SensorService(fragment.requireContext()).getDeviceOrientationSensor()
        orientation = DeviceOrientation.Orientation.Portrait
        fragment.observe(orientationSensor) {
            val newOrientation = orientationSensor.orientation
            if (newOrientation == DeviceOrientation.Orientation.Landscape || newOrientation == DeviceOrientation.Orientation.LandscapeInverse) {
                orientation = newOrientation
            } else if (newOrientation == DeviceOrientation.Orientation.Portrait || newOrientation == DeviceOrientation.Orientation.PortraitInverse) {
                orientation = newOrientation
            }
        }

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            takePhoto()
            true
        }

        dialog.setOnCancelListener { onCapture(null) }
        dialog.setOnDismissListener {
            stopCamera()
        }
        camera?.let {
            if (!isCameraStarted) {
                startCamera(it)
            }
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
            lifecycleOwner = fragment.viewLifecycleOwner,
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
        fragment.inBackground {
            val file = FileSubsystem.getInstance(fragment.requireContext()).createTemp("jpg")
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
                dismiss()
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun stopCamera() {
        camera?.stop()
        camera = null
        isCameraStarted = false
        dialog = null
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
                        .testTag("camera")
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
