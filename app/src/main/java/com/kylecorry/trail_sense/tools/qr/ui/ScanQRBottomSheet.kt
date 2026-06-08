package com.kylecorry.trail_sense.tools.qr.ui

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView

class ScanQRBottomSheet(
    private val fragment: Fragment,
    private val title: String,
    private val onTextScanned: (text: String?) -> Boolean
) {

    private val cameraSize = Size(200, 200)
    private val haptics by lazy { HapticSubsystem.getInstance(fragment.requireContext()) }
    private var camera: CameraState? = null
    private var lastMessage: String? = null
    private var sheet: BottomSheetDialog? = null

    fun show(): BottomSheetDialog {
        val context = fragment.requireContext()
        val dialog = BottomSheetDialog(context)
        sheet = dialog
        dialog.setContentView(
            TrailSenseComposeRootView(
                context,
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            ) {
                QRImportContent(
                    title = title,
                    onClose = {
                        onTextScanned(null)
                        sheet?.dismiss()
                    },
                    onCameraReady = {
                        camera = it
                        it.clipToOutline = true
                        it.start(cameraSize) { bitmap ->
                            onCameraUpdate(bitmap)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        dialog.setOnDismissListener {
            stopCamera()
            haptics.off()
            sheet = null
        }
        dialog.show()
        return dialog
    }

    private fun onCameraUpdate(bitmap: Bitmap) {
        if (camera == null) {
            bitmap.recycle()
            return
        }
        var message: String? = null
        tryOrNothing {
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        if (message != null && lastMessage != message) {
            haptics.click()
            lastMessage = message
            if (!onTextScanned(message)) {
                sheet?.dismiss()
            }
        }
    }

    private fun stopCamera() {
        camera?.stop()
        camera = null
    }
}

@Composable
private fun QRImportContent(
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
                    .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
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

@Composable
private fun rememberQrCameraState(onCameraReady: (CameraState) -> Unit): CameraState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val camera = androidx.compose.runtime.remember {
        CameraState(context).apply {
            setBackgroundResource(R.drawable.rounded_rectangle)
            setFlipEnabled(true)
        }
    }
    androidx.compose.runtime.LaunchedEffect(camera) {
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
