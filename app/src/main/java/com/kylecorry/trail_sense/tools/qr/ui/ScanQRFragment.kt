package com.kylecorry.trail_sense.tools.qr.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrNull
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScanQRFragment : TrailSenseComposeFragment() {

    private val cameraSize = Size(200, 200)

    private var history by mutableStateOf<List<String>>(emptyList())
    private var historyScrollRequest by mutableIntStateOf(0)
    private var cameraView: CameraState? = null
    private var shouldStartCamera = false

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private val scanLock = Any()

    @Composable
    override fun FragmentContent() {
        QrScannerScreen(
            history = history,
            scrollRequest = historyScrollRequest,
            onCameraReady = ::onCameraReady,
            onCopy = ::copy,
            onCreateNote = ::createNote,
            onOpenMap = ::openMap,
            onCreateBeacon = ::createBeacon,
            onOpenUrl = ::openUrl,
            onDelete = ::deleteResult
        )
    }

    private fun onCameraReady(cameraView: CameraState) {
        this.cameraView = cameraView
        cameraView.clipToOutline = true
        if (shouldStartCamera && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startCamera()
        }
    }

    private fun copy(text: String) {
        Clipboard.copy(requireContext(), text, getString(R.string.copied_to_clipboard_toast))
    }

    private fun createNote(text: String) {
        inBackground {
            val id = withContext(Dispatchers.IO) {
                NoteRepo.getInstance(requireContext())
                    .addNote(noteQREncoder.decode(text))
            }
            withContext(Dispatchers.Main) {
                CustomUiUtils.snackbar(
                    this@ScanQRFragment,
                    getString(R.string.saved_to_note),
                    action = getString(R.string.view)
                ) {
                    findNavController().navigate(
                        R.id.fragmentToolNotesCreate,
                        Bundle().apply {
                            putLong("edit_note_id", id)
                        }
                    )
                }
            }
        }
    }

    private fun openMap(text: String) {
        val intent = Intents.url(text)
        Intents.openChooser(requireContext(), intent, text)
    }

    private fun openUrl(text: String) {
        val protocols = listOf("http", "https", "rtsp", "ftp")
        val url = if (protocols.none { text.lowercase().startsWith(it) }) {
            "https://$text"
        } else {
            text
        }
        val intent = Intents.url(url)
        Intents.openChooser(requireContext(), intent, text)
    }

    private fun createBeacon(text: String) {
        val beacon = beaconQREncoder.decode(text) ?: return
        inBackground {
            val id = BeaconService(requireContext()).add(beacon)
            CustomUiUtils.snackbar(
                this@ScanQRFragment,
                getString(R.string.beacon_created),
                action = getString(R.string.view)
            ) {
                findNavController().navigate(
                    R.id.beaconDetailsFragment,
                    Bundle().apply {
                        putLong("beacon_id", id)
                    }
                )
            }
        }
    }

    private fun deleteResult(text: String) {
        val index = history.indexOf(text)
        if (index < 0) {
            return
        }

        history = history.filterNot { it == text }
        updateHistoryList(false)
        CustomUiUtils.snackbar(
            this@ScanQRFragment,
            getString(R.string.result_deleted),
            action = getString(R.string.undo)
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

    override fun onResume() {
        super.onResume()
        updateHistoryList()
        requestCamera { hasPermission ->
            if (hasPermission) {
                shouldStartCamera = true
                startCamera()
            } else {
                shouldStartCamera = false
                alertNoCameraPermission()
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
        if (!isAdded || view == null) {
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
            Alerts.toast(requireContext(), getString(R.string.no_qr_code_detected))
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

    override fun onPause() {
        super.onPause()
        shouldStartCamera = false
        cameraView?.stop()
        haptics.off()
    }

    override fun onDestroyView() {
        cameraView?.stop()
        cameraView = null
        super.onDestroyView()
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val results = history.ifEmpty { listOf("") }

    LaunchedEffect(scrollRequest) {
        listState.animateScrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val cameraState = remember {
                CameraState(context).apply {
                    setBackgroundResource(R.drawable.rounded_rectangle)
                }
            }
            LaunchedEffect(cameraState) {
                onCameraReady(cameraState)
            }
            CameraPreview(
                state = cameraState,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("camera")
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_result")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(type.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                        .testTag("qr_message_type"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = text.ifEmpty { stringResource(R.string.no_qr_code_detected) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .testTag("qr_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (text.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            if (text.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("qr_actions"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
}

@Composable
private fun QrActionChip(
    text: String,
    icon: Int,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier.testTag(tag),
        label = { Text(text) },
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
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
