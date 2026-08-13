package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

@Composable
fun QRImportContent(
    title: String,
    onClose: () -> Unit,
    onCameraReady: (CameraState) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.md,
                        top = TrailSenseSpacing.sm,
                        bottom = TrailSenseSpacing.sm
                    )
                    .testTag("tool_title"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.screen),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .testTag("camera"),
                    state = rememberQrCameraState(onCameraReady)
                )
            }
        }
    }
}

@Composable
private fun rememberQrCameraState(onCameraReady: (CameraState) -> Unit): CameraState {
    val context = LocalContext.current
    val camera = remember {
        CameraState(context).apply {
            setBackgroundResource(R.drawable.rounded_rectangle)
            setFlipEnabled(true)
        }
    }
    LaunchedEffect(camera) {
        onCameraReady(camera)
    }
    return camera
}

@Preview(showBackground = true)
@Composable
private fun QRImportContentPreview() {
    TrailSenseTheme {
        QRImportContent(
            title = "Scan QR",
            onClose = {},
            onCameraReady = {}
        )
    }
}
