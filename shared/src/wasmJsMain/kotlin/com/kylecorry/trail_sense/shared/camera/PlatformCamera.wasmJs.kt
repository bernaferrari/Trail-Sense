package com.kylecorry.trail_sense.shared.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.AppPermission

@Composable
actual fun rememberSharedPermissionRequester(): suspend (AppPermission) -> Boolean {
    return { false }
}

@Composable
actual fun SharedCameraPreview(
    modifier: Modifier,
    onReady: (SharedCameraController?) -> Unit,
    onError: (Throwable) -> Unit,
    frameAnalysis: Boolean
) {
    LaunchedEffect(Unit) {
        onReady(null)
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Live camera preview is not available on this platform.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

actual suspend fun saveCapturedPhotoToCache(bytes: ByteArray, prefix: String): String? = null
