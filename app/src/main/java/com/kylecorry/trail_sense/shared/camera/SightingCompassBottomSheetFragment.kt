package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.fragments.FullscreenDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.navigation.ui.LinearCompassView
import kotlinx.coroutines.Dispatchers

class SightingCompassBottomSheetFragment(
    private val onSelect: (bearing: Float?) -> Unit
) : FullscreenDialogFragment() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private var camera: CameraView? = null
    private var linearCompass: LinearCompassView? = null
    private var isViewCreated = false
    private var isCameraStarted = false
    private var bearingState by mutableStateOf<Float?>(null)

    var bearing: Float?
        get() = bearingState
        set(value) {
            bearingState = value
            updateCompass()
        }

    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Default)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SightingCompassContent(
                        bearingText = bearingState?.let { formatter.formatDegrees(it) } ?: "",
                        bearing = bearingState ?: 0f,
                        onClose = {
                            onSelect(null)
                            dismiss()
                        },
                        onCapture = ::confirmBearing,
                        onCameraReady = {
                            camera = it
                            if (isViewCreated) {
                                startCamera(it)
                            }
                            updateCompass()
                        },
                        onCompassReady = {
                            linearCompass = it
                            updateCompass()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars)
                    )
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            confirmBearing()
            true
        }

        dialog?.setOnCancelListener { onSelect(null) }
        camera?.let(::startCamera)
        updateCompass()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(camera: CameraView) {
        if (isCameraStarted) {
            return
        }

        isCameraStarted = true
        camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        camera.start(
            lifecycleOwner = this,
            readFrames = false,
            shouldStabilizePreview = false
        )
        updateCompassRange()
    }

    private fun updateCompass() {
        linearCompass?.azimuth = bearing ?: 0f
        updateCompassRange()
    }

    private fun updateCompassRange() {
        val camera = camera ?: return
        if (linearCompass == null) {
            return
        }

        inBackground {
            fovRunner.enqueue {
                if (
                    this@SightingCompassBottomSheetFragment.camera == null ||
                    this@SightingCompassBottomSheetFragment.linearCompass == null
                ) {
                    return@enqueue
                }
                val minimumFOV = 5f
                val range = camera.fov.first.coerceAtLeast(minimumFOV)
                onMain {
                    linearCompass?.range = range
                }
            }
        }
    }

    private fun confirmBearing() {
        onSelect(bearing)
        dismiss()
    }

    override fun onDestroyView() {
        camera?.stop()
        camera = null
        linearCompass = null
        isCameraStarted = false
        isViewCreated = false
        fovRunner.cancel()
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        camera?.stop()
    }
}

@Composable
private fun SightingCompassContent(
    bearingText: String,
    bearing: Float,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onCameraReady: (CameraView) -> Unit,
    onCompassReady: (LinearCompassView) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SightingCompassToolbar(
                title = bearingText,
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!isPreview) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera"),
                        factory = { context ->
                            CameraView(context, null).apply {
                                id = R.id.camera
                                onCameraReady(this)
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera")
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("view_camera_line")
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.Center)
                        .testTag("linear_compass"),
                    factory = { context ->
                        LinearCompassView(context, null).apply {
                            id = R.id.linear_compass
                            azimuth = bearing
                            onCompassReady(this)
                        }
                    },
                    update = {
                        it.azimuth = bearing
                    }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
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
private fun SightingCompassToolbar(
    title: String,
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
            text = title,
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
private fun SightingCompassContentPreview() {
    TrailSenseTheme {
        SightingCompassContent(
            bearingText = "123",
            bearing = 123f,
            onClose = {},
            onCapture = {},
            onCameraReady = {},
            onCompassReady = {}
        )
    }
}
