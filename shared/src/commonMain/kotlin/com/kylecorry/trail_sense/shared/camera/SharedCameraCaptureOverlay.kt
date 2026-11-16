package com.kylecorry.trail_sense.shared.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kylecorry.trail_sense.shared.sharing.pickImageFile
import kotlinx.coroutines.launch

@Composable
fun SharedCameraCaptureOverlay(
    onCaptured: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var camera by remember { mutableStateOf<SharedCameraController?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
        ) {
            SharedCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onReady = { controller ->
                    camera = controller
                    controller?.setTorch(torchOn)
                },
                onError = {
                    camera = null
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = {
                            torchOn = !torchOn
                            camera?.setTorch(torchOn)
                        },
                        enabled = camera != null
                    ) {
                        Icon(
                            if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = null
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { camera?.flip() },
                        enabled = camera != null
                    ) {
                        Icon(Icons.Default.FlipCameraIos, contentDescription = null)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (isCapturing) return@FilledTonalIconButton
                        isCapturing = true
                        scope.launch {
                            val picked = runCatching { pickImageFile() }.getOrNull()
                            val uri = if (picked != null) {
                                picked.path ?: saveCapturedPhotoToCache(picked.bytes, "picked")
                            } else {
                                null
                            }
                            isCapturing = false
                            if (uri != null) {
                                onCaptured(uri)
                            }
                        }
                    },
                    enabled = !isCapturing
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                }

                FilledIconButton(
                    onClick = {
                        val activeCamera = camera ?: return@FilledIconButton
                        if (isCapturing) return@FilledIconButton
                        isCapturing = true
                        scope.launch {
                            val uri = runCatching {
                                val bytes = activeCamera.capture()
                                saveCapturedPhotoToCache(bytes, "capture")
                            }.getOrNull()
                            isCapturing = false
                            onCaptured(uri)
                        }
                    },
                    enabled = camera != null && !isCapturing,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}
