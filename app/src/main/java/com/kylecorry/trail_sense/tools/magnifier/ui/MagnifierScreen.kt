@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.magnifier.ui

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCameraUnavailable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentPanel
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale

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
    var hasPermissionAnswer by remember { mutableStateOf(false) }
    var isCloseUpFocus by remember { mutableStateOf(false) }
    var frozenFrame by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        onRequestCamera {
            isCameraEnabled = it
            hasPermissionAnswer = true
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

    // Full-bleed camera under a small top bar; the two controls float in one tonal panel at the bottom.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_magnifier_title),
        modifier = modifier
    ) {
        if (hasPermissionAnswer && !isCameraEnabled) {
            TrailSenseCameraUnavailable()
            return@TrailSenseInstrumentScaffold
        }

        CameraPreview(
            state = cameraState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera")
        )

        frozenFrame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.camera_capture),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("frozen_frame"),
                contentScale = ContentScale.Crop
            )
        }

        TrailSenseInstrumentPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TrailSenseSpacing.xl)
                .testTag("magnifier_controls")
        ) {
            Row(
                modifier = Modifier.padding(TrailSenseSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MagnifierButton(
                    onClick = { isCloseUpFocus = !isCloseUpFocus },
                    selected = isCloseUpFocus,
                    modifier = Modifier.testTag("focus_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isCloseUpFocus) Icons.Default.Search else Icons.Default.CenterFocusStrong,
                        contentDescription = stringResource(R.string.camera)
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
                    selected = frozenFrame != null,
                    modifier = Modifier.testTag("freeze_btn")
                ) {
                    Icon(
                        imageVector = if (frozenFrame == null) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.camera_capture)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MagnifierButton(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "magnifierButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "magnifierButtonContent"
    )

    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(56.dp)
            .trailSensePressScale(interactionSource),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
