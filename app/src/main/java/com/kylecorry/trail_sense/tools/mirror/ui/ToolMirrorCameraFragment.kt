package com.kylecorry.trail_sense.tools.mirror.ui

import android.Manifest
import android.graphics.Color
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requirePermissionRequester
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment

class ToolMirrorCameraFragment : TrailSenseComposeFragment() {
    private val flashlight by lazy { ScreenTorch(requireActivity().window) }

    @Composable
    override fun FragmentContent() {
        MirrorCameraScreen(
            onRequestCamera = ::requestCameraPermission,
            onResumeScreen = { flashlight.on() },
            onPauseScreen = { flashlight.off() }
        )
    }

    private fun requestCameraPermission(action: (hasPermission: Boolean) -> Unit) {
        requirePermissionRequester().requestPermissions(listOf(Manifest.permission.CAMERA)) {
            val isAvailable = Permissions.isCameraEnabled(requireContext())
            if (!isAvailable) {
                alertNoCameraPermission()
            }
            action(isAvailable)
        }
    }
}

@Composable
private fun MirrorCameraScreen(
    onRequestCamera: ((Boolean) -> Unit) -> Unit,
    onResumeScreen: () -> Unit,
    onPauseScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraView = remember {
        CameraView(context, null).apply {
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
    DisposableEffect(lifecycleOwner, cameraView, isCameraEnabled) {
        fun startCamera() {
            if (isCameraEnabled) {
                cameraView.start(
                    readFrames = false,
                    preferBackCamera = false,
                    shouldStabilizePreview = false
                )
            } else {
                cameraView.stop()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onResumeScreen()
                    startCamera()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    cameraView.stop()
                    onPauseScreen()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        startCamera()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraView.stop()
            onPauseScreen()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { cameraView },
            modifier = Modifier
                .fillMaxSize(0.9f)
                .padding(0.dp)
                .testTag("camera")
        )
    }
}
