@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.magnifier.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.Color
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.TorchOff
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

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
            // The torch is a top bar action, not a button floating over the picture.
            setShowTorch(false)
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

    // One question: what does this look like up close?
    // The picture is the instrument: one rounded window on the plain page. Under it, the zoom on one slider and the
    // two toggles side by side; the torch is a top bar action. Nothing floats over the picture.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_magnifier_title),
        modifier = modifier,
        actions = {
            if (isCameraEnabled) {
                // A toggle that fills when the torch is on, so its state reads at a glance.
                FilledTonalIconToggleButton(
                    checked = cameraState.torchOn,
                    onCheckedChange = { cameraState.setTorch(it) },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painter = rememberVectorPainter(
                            if (cameraState.torchOn) TrailIcons.TorchOn else TrailIcons.TorchOff
                        ),
                        contentDescription = stringResource(R.string.flashlight_title)
                    )
                }
            }
        }
    ) {
        if (hasPermissionAnswer && !isCameraEnabled) {
            MagnifierCameraUnavailable(
                onRequestCamera = {
                    onRequestCamera { isCameraEnabled = it }
                }
            )
            return@TrailSenseInstrumentScaffold
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                CameraPreview(
                    state = cameraState,
                    showZoomSlider = false,
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
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.sm,
                        bottom = TrailSenseSpacing.lg
                    )
                    .testTag("magnifier_controls"),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                ZoomSlider(
                    zoom = cameraState.zoomValue.coerceIn(0f, 1f),
                    onZoom = { cameraState.setZoomFromUser(it.coerceIn(0f, 1f)) },
                    // A frozen picture doesn't zoom.
                    enabled = frozenFrame == null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                    MagnifierToggle(
                        checked = isCloseUpFocus,
                        onCheckedChange = { isCloseUpFocus = it },
                        label = stringResource(R.string.magnifier_close_up),
                        icon = Icons.Default.CenterFocusStrong,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("focus_toggle_btn")
                    )
                    TrailSenseButton(
                        text = stringResource(
                            if (frozenFrame == null) R.string.magnifier_freeze else R.string.magnifier_live
                        ),
                        icon = if (frozenFrame == null) Icons.Default.Pause else Icons.Default.PlayArrow,
                        style = TrailSenseButtonStyle.Filled,
                        onClick = {
                            frozenFrame = if (frozenFrame == null) cameraState.previewImage else null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("freeze_btn")
                    )
                }
            }
        }
    }
}

/** The zoom, between a zoom-out and a zoom-in button that step it by a tenth. */
@Composable
private fun ZoomSlider(zoom: Float, onZoom: (Float) -> Unit, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onZoom(zoom - ZOOM_STEP) }, enabled = enabled) {
            Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.zoom_out))
        }
        Slider(
            value = zoom,
            onValueChange = onZoom,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.xs),
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    enabled = enabled,
                    drawStopIndicator = null
                )
            }
        )
        IconButton(onClick = { onZoom(zoom + ZOOM_STEP) }, enabled = enabled) {
            Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.zoom_in))
        }
    }
}

/** The close-up toggle: tonal when off, solid primary when on. */
@Composable
private fun MagnifierToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val height = ButtonDefaults.MediumContainerHeight
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.heightIn(min = height),
        colors = ToggleButtonDefaults.tonalToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(height)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        Text(label, style = ButtonDefaults.textStyleFor(height))
    }
}

private const val ZOOM_STEP = 0.1f
