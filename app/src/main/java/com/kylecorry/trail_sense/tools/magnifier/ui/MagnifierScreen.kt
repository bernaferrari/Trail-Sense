@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.magnifier.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState

@Composable
internal fun MagnifierScreen(
    onRequestCamera: ((Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraState = remember {
        CameraState(context).apply {
            defaultZoomRatio = 2f
            minZoomRatio = 1f
            setShowTorch(true)
            keepScreenOn = true
        }
    }
    var isCameraEnabled by remember { mutableStateOf(false) }
    var isCloseUpFocus by remember { mutableStateOf(false) }
    var frozenFrame by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        onRequestCamera {
            isCameraEnabled = it
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, cameraState, isCameraEnabled) {
        fun startCamera() {
            if (isCameraEnabled) {
                cameraState.start(
                    readFrames = false,
                    preferBackCamera = true,
                    shouldStabilizePreview = false
                )
            } else {
                cameraState.stop()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startCamera()
                Lifecycle.Event.ON_PAUSE -> cameraState.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        startCamera()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraState.stop()
        }
    }

    LaunchedEffect(isCloseUpFocus, isCameraEnabled) {
        if (isCameraEnabled) {
            cameraState.setFocus(if (isCloseUpFocus) 0f else null)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        CameraPreview(
            state = cameraState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera")
        )

        frozenFrame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("frozen_frame"),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .testTag("magnifier_controls"),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MagnifierButton(
                onClick = { isCloseUpFocus = !isCloseUpFocus },
                modifier = Modifier.testTag("focus_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isCloseUpFocus) Icons.Default.Search else Icons.Default.CenterFocusStrong,
                    contentDescription = null
                )
            }
            MagnifierButton(
                onClick = {
                    frozenFrame = if (frozenFrame == null) {
                        cameraState.previewImage
                    } else {
                        null
                    }
                },
                modifier = Modifier.testTag("freeze_btn")
            ) {
                Icon(
                    imageVector = if (frozenFrame == null) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun MagnifierButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        ),
        content = content
    )
}
