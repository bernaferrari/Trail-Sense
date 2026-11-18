package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.toBeacon
import com.kylecorry.trail_sense.shared.camera.SharedCameraController
import com.kylecorry.trail_sense.shared.camera.SharedCameraPreview
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.qr_code_scanner
import com.kylecorry.trail_sense.shared.sharing.copyToClipboard
import com.kylecorry.trail_sense.shared.sharing.openExternalUrl
import com.kylecorry.trail_sense.shared.sharing.pickImageFile
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.notes.infrastructure.INoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.decodeQrFromFrame
import com.kylecorry.trail_sense.tools.qr.infrastructure.decodeQrFromImageBytes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SharedScanQRScreen(
    onBack: () -> Unit = {},
    onOpenBeacon: (Long) -> Unit = {},
    onOpenNote: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf<Boolean?>(null) }
    var camera by remember { mutableStateOf<SharedCameraController?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        hasPermission = effects.requestPermission(AppPermission.Camera)
    }

    LaunchedEffect(camera, scannedValue) {
        val activeCamera = camera ?: return@LaunchedEffect
        if (scannedValue != null) return@LaunchedEffect
        activeCamera.frames.collectLatest { frame ->
            val decoded = decodeQrFromFrame(frame.width, frame.height, frame.argb)
            if (!decoded.isNullOrBlank()) {
                scannedValue = decoded
            }
        }
    }

    TrailSenseInstrumentScaffold(
        title = appString(Res.string.qr_code_scanner),
        onBack = onBack,
        actions = {
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
                onClick = {
                    scope.launch {
                        val file = pickImageFile() ?: return@launch
                        val decoded = decodeQrFromImageBytes(file.bytes)
                        if (decoded.isNullOrBlank()) {
                            effects.showMessage("No QR code found in image")
                        } else {
                            scannedValue = decoded
                        }
                    }
                }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission == true) {
                    SharedCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onReady = { controller ->
                            camera = controller
                            controller?.setTorch(torchOn)
                        },
                        frameAnalysis = true
                    )
                } else if (hasPermission == false) {
                    Text(
                        text = "Camera permission denied. You can still import a QR image from Photos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (scanMessage != null) {
                Text(
                    text = scanMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val file = pickImageFile() ?: return@launch
                        val decoded = decodeQrFromImageBytes(file.bytes)
                        if (decoded.isNullOrBlank()) {
                            effects.showMessage("No QR code found in image")
                        } else {
                            scannedValue = decoded
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Text("  Import QR from image")
            }
        }
    }

    val activeScan = scannedValue
    if (activeScan != null) {
        val noteEncoder = remember { NoteQREncoder() }
        val parsedBeacon = remember(activeScan) {
            if (activeScan.startsWith("geo:", ignoreCase = true)) {
                runCatching { GeoUri.parse("geo:" + activeScan.substringAfter(':'))?.toBeacon() }.getOrNull()
            } else null
        }
        val isUrl = remember(activeScan) {
            activeScan.startsWith("http://", ignoreCase = true) ||
                activeScan.startsWith("https://", ignoreCase = true)
        }
        val parsedNote = remember(activeScan, isUrl) {
            if (isUrl) null else noteEncoder.decodeTitledNote(activeScan)
        }

        AlertDialog(
            onDismissRequest = { scannedValue = null },
            title = {
                Text(
                    when {
                        parsedBeacon != null -> "Beacon QR"
                        parsedNote != null -> "Note QR"
                        else -> "QR Code"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            parsedBeacon != null -> parsedBeacon.name
                            parsedNote != null -> parsedNote.title ?: parsedNote.contents.orEmpty()
                            else -> activeScan
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = activeScan
                        scannedValue = null
                        scope.launch {
                            when {
                                parsedBeacon != null -> {
                                    val id = getAppService<IBeaconService>().add(parsedBeacon)
                                    effects.showMessage("Beacon imported")
                                    onOpenBeacon(id)
                                }
                                parsedNote != null -> {
                                    val id = getAppService<INoteRepo>().addNote(parsedNote)
                                    effects.showMessage("Note imported")
                                    onOpenNote(id)
                                }
                                isUrl -> {
                                    openExternalUrl(value)
                                }
                                else -> {
                                    val copied = copyToClipboard(value)
                                    effects.showMessage(if (copied) "Copied to clipboard" else "Could not copy")
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            parsedBeacon != null || parsedNote != null -> "Import"
                            isUrl -> "Open"
                            else -> "Copy"
                        }
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (parsedBeacon == null && parsedNote == null) {
                        TextButton(onClick = {
                            scannedValue = null
                            scope.launch {
                                val id = getAppService<INoteRepo>().addNote(noteEncoder.decode(activeScan))
                                effects.showMessage("Note imported")
                                onOpenNote(id)
                            }
                        }) {
                            Text("Save as note")
                        }
                    }
                    TextButton(onClick = { scannedValue = null }) {
                        Text(systemString(SystemString.Cancel))
                    }
                }
            }
        )
    }
}
