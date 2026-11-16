package com.kylecorry.trail_sense.tools.mirror.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.camera.SharedCameraController
import com.kylecorry.trail_sense.shared.camera.SharedCameraPreview
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.mirror_camera
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold

private val MirrorCorner = 56.dp

@Composable
fun SharedMirrorCameraScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effects = LocalScreenEffects.current
    var hasPermission by remember { mutableStateOf<Boolean?>(null) }
    var camera by remember { mutableStateOf<SharedCameraController?>(null) }
    var flippedToFront by remember { mutableStateOf(false) }
    var screenTorch by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        hasPermission = effects.requestPermission(AppPermission.Camera)
    }

    val backgroundColor = if (screenTorch) Color.White else MaterialTheme.colorScheme.background

    TrailSenseInstrumentScaffold(
        title = appString(Res.string.mirror_camera),
        onBack = onBack,
        actions = {
            FilledTonalIconButton(onClick = { screenTorch = !screenTorch }) {
                Icon(
                    if (screenTorch) Icons.Filled.LightMode else Icons.Outlined.LightMode,
                    contentDescription = null
                )
            }
            FilledTonalIconButton(
                onClick = { camera?.flip() },
                enabled = camera != null
            ) {
                Icon(Icons.Default.FlipCameraIos, contentDescription = null)
            }
        },
        modifier = modifier.background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(if (screenTorch) 24.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                hasPermission == false -> {
                    Text(
                        text = "Camera permission is required to use the mirror camera.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                cameraError != null -> {
                    Text(
                        text = cameraError ?: "Camera unavailable",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                hasPermission == true -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(MirrorCorner))
                    ) {
                        SharedCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onReady = { controller ->
                                camera = controller
                                if (controller != null && !flippedToFront) {
                                    flippedToFront = true
                                    controller.flip()
                                }
                            },
                            onError = { err ->
                                cameraError = err.message ?: "Camera unavailable"
                            }
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = zoom,
                                onValueChange = { value ->
                                    zoom = value
                                    camera?.setZoom(value)
                                },
                                valueRange = 1f..4f
                            )
                        }
                    }
                }
            }
        }
    }
}
