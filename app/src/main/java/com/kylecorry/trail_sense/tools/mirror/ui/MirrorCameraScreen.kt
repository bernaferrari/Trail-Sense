@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.mirror.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.rememberCoroutineScope
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.camera.CameraScaleType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.R
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCameraUnavailable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.andromeda.torch.ScreenTorch

@Composable
internal fun MirrorCameraScreen(
    onRequestCamera: ((Boolean) -> Unit) -> Unit,
    onResumeScreen: () -> Unit,
    onPauseScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraState = remember {
        CameraState(context).apply {
            setScaleType(CameraScaleType.Fill)
            setShowTorch(false)
            setPreviewBackgroundColor(ComposeColor.White)
            keepScreenOn = true
        }
    }
    var isCameraEnabled by remember {
        mutableStateOf(Permissions.isCameraEnabled(context))
    }
    var wasPermissionRequested by remember { mutableStateOf(false) }
    var wasPermissionAnswered by remember { mutableStateOf(false) }
    val cameraDescription = stringResource(R.string.camera)

    LaunchedEffect(Unit) {
        if (!wasPermissionRequested) {
            wasPermissionRequested = true
            onRequestCamera {
                isCameraEnabled = it
                wasPermissionAnswered = true
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, cameraState, isCameraEnabled) {
        fun startCamera() {
            if (isCameraEnabled) {
                cameraState.start(
                    readFrames = false,
                    preferBackCamera = false,
                    shouldStabilizePreview = false
                )
            } else {
                cameraState.stop()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onResumeScreen()
                    startCamera()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    cameraState.stop()
                    onPauseScreen()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        startCamera()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraState.stop()
            onPauseScreen()
        }
    }

    // The white page is the mirror's light; the preview is a deeply rounded glass, like a compact mirror.
    // It stays white around the preview, top bar included.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.mirror_camera),
        modifier = modifier,
        containerColor = ComposeColor.White,
        contentColor = ComposeColor.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xl
                ),
            contentAlignment = Alignment.Center
        ) {
            if (wasPermissionAnswered && !isCameraEnabled) {
                // On the theme's own surface so the message stays legible on the white light in dark mode.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(MirrorCorner),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    TrailSenseCameraUnavailable()
                }
            } else {
                CameraPreview(
                    state = cameraState,
                    modifier = Modifier
                        .widthIn(max = TrailSenseSize.maximumContentWidth)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(MirrorCorner))
                        .testTag("camera")
                        .semantics { contentDescription = cameraDescription }
                )
            }
        }
    }
}


/** The mirror glass: rounder than a card so it reads as an object, not a panel. */
private val MirrorCorner = 56.dp

/** The mirror: the front camera, with the screen lit white while it is shown. */
@Composable
fun MirrorCameraToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    // The screen torch lights the activity's window.
    val window = requireNotNull(LocalActivity.current).window
    val flashlight = remember(window) { ScreenTorch(window) }
    MirrorCameraScreen(
        onRequestCamera = { action ->
            scope.launch {
                effects.requestPermission(AppPermission.Camera)
                val isAvailable = Permissions.isCameraEnabled(context)
                if (!isAvailable) {
                    effects.explainPermissionDenied(AppPermission.Camera)
                }
                action(isAvailable)
            }
        },
        onResumeScreen = { flashlight.on() },
        onPauseScreen = { flashlight.off() }
    )
}
