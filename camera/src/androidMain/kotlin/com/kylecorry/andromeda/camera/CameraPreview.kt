package com.kylecorry.andromeda.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

internal class CameraPreviewState(val fill: Boolean = true) {
    internal var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
    internal var size by mutableStateOf(IntSize.Zero)
}

@Composable
internal fun rememberCameraPreviewState(fill: Boolean = true): CameraPreviewState = remember(fill) { CameraPreviewState(fill) }

@Composable
internal fun CameraPreview(state: CameraPreviewState, modifier: Modifier = Modifier) {
    val request = state.surfaceRequest ?: return
    CameraXViewfinder(
        surfaceRequest = request,
        modifier = modifier.onSizeChanged { state.size = it },
        contentScale = if (state.fill) ContentScale.Crop else ContentScale.Fit,
    )
}
