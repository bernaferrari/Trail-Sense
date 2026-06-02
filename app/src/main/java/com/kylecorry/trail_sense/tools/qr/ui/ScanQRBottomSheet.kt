package com.kylecorry.trail_sense.tools.qr.ui

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class ScanQRBottomSheet(
    private val title: String,
    private val onTextScanned: (text: String?) -> Boolean
) : BottomSheetDialogFragment() {

    private val cameraSize = Size(200, 200)
    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private var camera: CameraView? = null
    private var lastMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    QRImportContent(
                        title = title,
                        onClose = {
                            onTextScanned(null)
                            dismiss()
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
            }
        }
    }

    override fun onDestroyView() {
        camera?.stop()
        camera = null
        super.onDestroyView()
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
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        camera?.stop()
        haptics.off()
    }
}

@Composable
private fun QRImportContent(
    title: String,
    onClose: () -> Unit,
    onCameraReady: (CameraView) -> Unit,
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
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("camera"),
                factory = { context ->
                    CameraView(context, null).apply {
                        id = R.id.camera
                        setBackgroundResource(R.drawable.rounded_rectangle)
                        setFlipEnabled(true)
                        onCameraReady(this)
                    }
                }
            )
        }
    }
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
