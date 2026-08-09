@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.mirror.ui

import android.graphics.Color
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState

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
            setScaleType(PreviewView.ScaleType.FIT_CENTER)
            setShowTorch(false)
            setPreviewBackgroundColor(Color.WHITE)
            keepScreenOn = true
        }
    }
    var isCameraEnabled by remember {
        mutableStateOf(Permissions.isCameraEnabled(context))
    }
    var wasPermissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!wasPermissionRequested) {
            wasPermissionRequested = true
            onRequestCamera {
                isCameraEnabled = it
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White)
            .padding(TrailSenseSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        // Tonal frame floating over the bright mirror backdrop. The camera
        // canvas itself is left untouched; only the surrounding chrome is styled.
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            CameraPreview(
                state = cameraState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .testTag("camera")
            )
        }
    }
}
