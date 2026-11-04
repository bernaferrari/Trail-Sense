package com.kylecorry.trail_sense.tools.qr.ui

import com.kylecorry.trail_sense.shared.views.roundedRectangleBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * QR import, shown inside a bottom sheet: the instructions with a close button, then a square viewfinder. Drawn on
 * the sheet's own surface rather than a second background.
 */
@Composable
fun QRImportContent(
    title: String,
    onClose: () -> Unit,
    onCameraReady: (CameraState) -> Unit,
    modifier: Modifier = Modifier
) {
    // The sheet supplies the side and bottom padding
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tool_title"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.xs)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(android.R.string.cancel)
                )
            }
        }
        CameraPreview(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .testTag("camera"),
            state = rememberQrCameraState(onCameraReady)
        )
    }
}

@Composable
private fun rememberQrCameraState(onCameraReady: (CameraState) -> Unit): CameraState {
    val context = LocalContext.current
    val camera = remember {
        CameraState(context).apply {
            setBackground(roundedRectangleBackground(context))
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
