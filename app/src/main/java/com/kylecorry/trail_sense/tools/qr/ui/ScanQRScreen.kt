@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.qr.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScanQRToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { ScanQRController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    QrScannerScreen(
        history = controller.history,
        scrollRequest = controller.historyScrollRequest,
        onCameraReady = controller::onCameraReady,
        onCopy = controller::copy,
        onCreateNote = controller::createNote,
        onOpenMap = controller::openMap,
        onCreateBeacon = controller::createBeacon,
        onOpenUrl = controller::openUrl,
        onDelete = controller::deleteResult,
        onScanFromGallery = controller::scanFromGallery
    )
}

private class ScanQRController(private val host: TrailSenseDestinationHost) {
    private val cameraSize = Size(200, 200)

    var history by mutableStateOf<List<String>>(emptyList())
        private set
    var historyScrollRequest by mutableIntStateOf(0)
        private set
    private var cameraView: CameraState? = null
    private var shouldStartCamera = false

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()

    private val haptics by lazy { HapticSubsystem.getInstance(host.requireContext()) }
    private val scanLock = Any()

    fun onCameraReady(cameraView: CameraState) {
        this.cameraView = cameraView
        cameraView.clipToOutline = true
        if (shouldStartCamera && host.lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startCamera()
        }
    }

    fun copy(text: String) {
        Clipboard.copy(
            host.requireContext(),
            text,
            host.getString(R.string.copied_to_clipboard_toast)
        )
    }

    fun createNote(text: String) {
        host.inBackground {
            val id = withContext(Dispatchers.IO) {
                NoteRepo.getInstance(host.requireContext())
                    .addNote(noteQREncoder.decode(text))
            }
            withContext(Dispatchers.Main) {
                CustomUiUtils.snackbar(
                    host,
                    host.getString(R.string.saved_to_note),
                    action = host.getString(R.string.view)
                ) {
                    host.findNavController().navigate(
                        R.id.fragmentToolNotesCreate,
                        Bundle().apply {
                            putLong("edit_note_id", id)
                        }
                    )
                }
            }
        }
    }

    fun openMap(text: String) {
        val intent = Intents.url(text)
        Intents.openChooser(host.requireContext(), intent, text)
    }

    fun openUrl(text: String) {
        val protocols = listOf("http", "https", "rtsp", "ftp")
        val url = if (protocols.none { text.lowercase().startsWith(it) }) {
            "https://$text"
        } else {
            text
        }
        val intent = Intents.url(url)
        Intents.openChooser(host.requireContext(), intent, text)
    }

    fun createBeacon(text: String) {
        val beacon = beaconQREncoder.decode(text) ?: return
        host.inBackground {
            val id = BeaconService(host.requireContext()).add(beacon)
            CustomUiUtils.snackbar(
                host,
                host.getString(R.string.beacon_created),
                action = host.getString(R.string.view)
            ) {
                host.findNavController().navigate(
                    R.id.beaconDetailsFragment,
                    Bundle().apply {
                        putLong("beacon_id", id)
                    }
                )
            }
        }
    }

    fun deleteResult(text: String) {
        val index = history.indexOf(text)
        if (index < 0) {
            return
        }

        history = history.filterNot { it == text }
        updateHistoryList(false)
        CustomUiUtils.snackbar(
            host,
            host.getString(R.string.result_deleted),
            action = host.getString(R.string.undo)
        ) {
            if (!history.contains(text)) {
                val restored = history.toMutableList()
                if (index <= restored.size) {
                    restored.add(index, text)
                } else {
                    restored.add(text)
                }
                history = restored
                updateHistoryList(false)
            }
        }
    }

    fun scanFromGallery() {
        host.inBackground(BackgroundMinimumState.Created) {
            val uri = IntentUriPicker(host.intentResultRetriever, host.requireContext())
                .open(listOf("image/*"))
                .getOrNull() ?: return@inBackground

            val indicator = AlertLoadingIndicator(
                host.requireContext(),
                host.getString(R.string.scanning_image)
            )
            try {
                indicator.show()
                val path = withContext(Dispatchers.IO) {
                    FileSubsystem.getInstance(host.requireContext()).copyToTemp(uri)
                }?.path ?: return@inBackground

                val bitmap = withContext(Dispatchers.IO) {
                    BitmapUtils.decodeBitmapScaled(path, mediaMaxWidth, mediaMaxHeight)
                } ?: return@inBackground

                DeleteTempFilesCommand(host.requireContext()).execute()
                onCameraUpdate(bitmap, isFromGallery = true)
            } finally {
                indicator.hide()
            }
        }
    }

    fun onResume() {
        updateHistoryList()
        host.requestCamera { hasPermission ->
            if (hasPermission) {
                shouldStartCamera = true
                startCamera()
            } else {
                shouldStartCamera = false
                host.alertNoCameraPermission()
            }
        }
    }

    private fun startCamera() {
        cameraView?.start(cameraSize) {
            onCameraUpdate(it)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(
        bitmap: Bitmap,
        isFromGallery: Boolean = false
    ): Boolean = synchronized(scanLock) {
        if (!host.lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            bitmap.recycle()
            return true
        }
        var message: String? = null
        try {
            message = QR.decode(bitmap, highAccuracy = isFromGallery)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bitmap.recycle()
        }

        if (message != null) {
            onQRScanned(message)
        } else if (isFromGallery) {
            Alerts.toast(
                host.requireContext(),
                host.getString(R.string.no_qr_code_detected)
            )
        }

        return true
    }

    private fun addReading(text: String) {
        val updated = history.toMutableList()
        if (updated.contains(text)) {
            updated.remove(text)
        }

        updated.add(0, text)

        while (updated.size > 10) {
            updated.removeLastOrNull()
        }

        history = updated
        updateHistoryList()
    }

    private fun updateHistoryList(scrollToTop: Boolean = true) {
        if (scrollToTop) {
            historyScrollRequest++
        }
    }

    private fun onQRScanned(message: String) {
        val lastMessage = history.firstOrNull()
        if (message.isNotEmpty() && lastMessage != message) {
            addReading(message)
            haptics.click()
        }
    }

    fun onPause() {
        shouldStartCamera = false
        cameraView?.stop()
        haptics.off()
    }

    fun onDestroy() {
        cameraView?.stop()
        cameraView = null
    }

    companion object {
        const val mediaMaxWidth = 500
        const val mediaMaxHeight = 500
    }
}

@Composable
private fun QrScannerScreen(
    history: List<String>,
    scrollRequest: Int,
    onCameraReady: (CameraState) -> Unit,
    onCopy: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onCreateBeacon: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDelete: (String) -> Unit,
    onScanFromGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val results = history.ifEmpty { listOf("") }
    val scannerDescription = stringResource(R.string.qr_code_scanner)

    LaunchedEffect(scrollRequest) {
        listState.animateScrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val cameraState = remember {
            CameraState(context).apply {
                setBackgroundResource(R.drawable.rounded_rectangle)
            }
        }
        LaunchedEffect(cameraState) {
            onCameraReady(cameraState)
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = TrailSenseSpacing.screen,
                    vertical = TrailSenseSpacing.xl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
            // Camera viewfinder framed by a tonal card. The AndroidView canvas
            // itself is left untouched — only the surrounding chrome is styled.
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    CameraPreview(
                        state = cameraState,
                        modifier = Modifier
                            .padding(TrailSenseSpacing.lg)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .testTag("camera")
                            .semantics { contentDescription = scannerDescription }
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = onScanFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery")
                ) {
                    Text(stringResource(R.string.select_from_gallery))
                }
            }

            items(results) { text ->
                QrResultCard(
                    text = text,
                    onCopy = onCopy,
                    onCreateNote = onCreateNote,
                    onOpenMap = onOpenMap,
                    onCreateBeacon = onCreateBeacon,
                    onOpenUrl = onOpenUrl,
                    onDelete = onDelete
                )
            }
        }
        }
    }
}

@Composable
private fun QrResultCard(
    text: String,
    onCopy: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onCreateBeacon: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val type = getScanType(text)
    val isEmpty = text.isEmpty()
    TrailSenseCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_result")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tonal circular icon chip indicating the detected content type.
            Surface(
                shape = CircleShape,
                color = if (isEmpty) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(type.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("qr_message_type"),
                        tint = if (isEmpty) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.size(TrailSenseSpacing.md))
            Text(
                text = text.ifEmpty { stringResource(R.string.no_qr_code_detected) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("qr_text"),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEmpty) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (!isEmpty) {
            Spacer(modifier = Modifier.size(TrailSenseSpacing.lg))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_actions"),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                if (type == ScanType.Url) {
                    QrActionChip(
                        text = stringResource(R.string.open),
                        icon = R.drawable.ic_link,
                        tag = "qr_web",
                        onClick = { onOpenUrl(text) }
                    )
                }

                if (type == ScanType.Geo) {
                    QrActionChip(
                        text = stringResource(R.string.beacon),
                        icon = R.drawable.ic_location,
                        tag = "qr_beacon",
                        onClick = { onCreateBeacon(text) }
                    )
                    QrActionChip(
                        text = stringResource(R.string.maps),
                        icon = R.drawable.maps,
                        tag = "qr_location",
                        onClick = { onOpenMap(text) }
                    )
                }

                if (type == ScanType.Text) {
                    QrActionChip(
                        text = stringResource(R.string.note),
                        icon = R.drawable.ic_tool_notes,
                        tag = "qr_save_note",
                        onClick = { onCreateNote(text) }
                    )
                }

                QrActionChip(
                    text = stringResource(android.R.string.copy),
                    icon = R.drawable.ic_copy,
                    tag = "qr_copy",
                    onClick = { onCopy(text) }
                )
                QrActionChip(
                    text = stringResource(R.string.delete),
                    icon = R.drawable.ic_delete,
                    tag = "qr_delete",
                    onClick = { onDelete(text) }
                )
            }
        }
    }
}

@Composable
private fun QrActionChip(
    text: String,
    icon: Int,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(TrailSenseSize.minimumTouchTarget)
            .testTag(tag)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(TrailSenseSpacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Normal
        )
    }
}

private enum class ScanType {
    Text,
    Url,
    Geo
}

private val ScanType.icon: Int
    get() = when (this) {
        ScanType.Text -> R.drawable.ic_tool_notes
        ScanType.Url -> R.drawable.ic_link
        ScanType.Geo -> R.drawable.ic_location
    }

private fun getScanType(text: String): ScanType {
    return when {
        isURL(text) -> ScanType.Url
        isLocation(text) -> ScanType.Geo
        else -> ScanType.Text
    }
}

private fun isLocation(text: String): Boolean {
    return GeoUri.from(text.toUri()) != null
}

private fun isURL(text: String): Boolean {
    return android.util.Patterns.WEB_URL.matcher(text).matches()
}
