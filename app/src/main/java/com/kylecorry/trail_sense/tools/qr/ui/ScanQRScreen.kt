@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.kylecorry.trail_sense.shared.icons.trail.QrCode
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.roundedRectangleBackground
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Link
import com.kylecorry.trail_sense.shared.icons.trail.Location
import com.kylecorry.trail_sense.shared.icons.trail.Maps
import com.kylecorry.trail_sense.shared.icons.trail.ToolNotes
import com.kylecorry.trail_sense.shared.icons.trail.Copy
import com.kylecorry.trail_sense.shared.icons.trail.Delete
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
        isCameraDenied = controller.isCameraDenied,
        onOpenAppSettings = controller::openAppSettings,
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
    var isCameraDenied by mutableStateOf(false)
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
            isCameraDenied = !hasPermission
            if (hasPermission) {
                shouldStartCamera = true
                startCamera()
            } else {
                // The scanner shows why the camera is missing in place of the viewfinder.
                shouldStartCamera = false
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

    fun openAppSettings() {
        host.startActivity(Intents.appSettings(host.requireContext()))
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

/**
 * The scanner: the viewfinder is the instrument, the gallery is the fallback, and the last ten codes sit below as
 * one group, newest first, each with the actions its content allows.
 */
@Composable
private fun QrScannerScreen(
    history: List<String>,
    scrollRequest: Int,
    isCameraDenied: Boolean,
    onOpenAppSettings: () -> Unit,
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
    val context = LocalContext.current
    val cameraState = remember {
        CameraState(context).apply {
            setBackground(roundedRectangleBackground(context))
        }
    }

    LaunchedEffect(cameraState) {
        onCameraReady(cameraState)
    }

    LaunchedEffect(scrollRequest) {
        listState.animateScrollToItem(0)
    }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.qr_code_scanner),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.xxxl
                ),
                // Results touch as one group (2 dp); the other items add their own room above.
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                item(key = "camera") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(TrailSenseShape.hero))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        // Keep the camera view attached so it can start the moment permission arrives.
                        CameraPreview(
                            state = cameraState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("camera")
                                .semantics { contentDescription = scannerDescription }
                        )
                        if (!isCameraDenied) {
                            ScanFrame(Modifier.fillMaxSize())
                        }
                        if (isCameraDenied) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                TrailSenseEmptyState(
                                    icon = Icons.Rounded.NoPhotography,
                                    title = stringResource(R.string.no_camera_access),
                                    description = stringResource(R.string.qr_camera_denied_description),
                                    actionLabel = stringResource(R.string.open_settings),
                                    onAction = onOpenAppSettings
                                )
                            }
                        }
                    }
                }

                item(key = "gallery") {
                    TrailSenseButton(
                        text = stringResource(R.string.select_from_gallery),
                        onClick = onScanFromGallery,
                        icon = Icons.Rounded.PhotoLibrary,
                        style = TrailSenseButtonStyle.Tonal,
                        modifier = Modifier
                            .padding(top = TrailSenseSpacing.lg)
                            .fillMaxWidth()
                            .testTag("gallery")
                    )
                }

                item(key = "results_header") {
                    TrailSenseGroupHeader(
                        title = stringResource(R.string.history),
                        modifier = Modifier.animateItem()
                    )
                }

                itemsIndexed(results, key = { _, text -> "result:$text" }) { index, text ->
                    QrResultCard(
                        text = text,
                        position = TrailSenseGroupPosition.at(index, results.size),
                        onCopy = onCopy,
                        onCreateNote = onCreateNote,
                        onOpenMap = onOpenMap,
                        onCreateBeacon = onCreateBeacon,
                        onOpenUrl = onOpenUrl,
                        onDelete = onDelete,
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QrResultCard(
    text: String,
    position: TrailSenseGroupPosition,
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
    TrailSenseGroupCard(
        position = position,
        modifier = modifier
            .testTag("qr_result")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TrailSenseShapeBadge(
                icon = if (isEmpty) TrailIcons.QrCode else type.icon,
                shape = type.shape.toShape(),
                size = 48.dp,
                containerColor = if (isEmpty) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    type.container()
                },
                contentColor = if (isEmpty) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    type.content()
                },
                modifier = Modifier.testTag("qr_message_type")
            )
            Spacer(modifier = Modifier.width(TrailSenseSpacing.md))
            Text(
                text = text.ifEmpty { stringResource(R.string.no_qr_code_detected) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("qr_text"),
                style = if (isEmpty) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                color = if (isEmpty) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (!isEmpty) {
            Spacer(modifier = Modifier.size(TrailSenseSpacing.md))
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
                        icon = TrailIcons.Link,
                        tag = "qr_web",
                        primary = true,
                        onClick = { onOpenUrl(text) }
                    )
                }

                if (type == ScanType.Geo) {
                    QrActionChip(
                        text = stringResource(R.string.beacon),
                        icon = TrailIcons.Location,
                        tag = "qr_beacon",
                        primary = true,
                        onClick = { onCreateBeacon(text) }
                    )
                    QrActionChip(
                        text = stringResource(R.string.maps),
                        icon = TrailIcons.Maps,
                        tag = "qr_location",
                        onClick = { onOpenMap(text) }
                    )
                }

                if (type == ScanType.Text) {
                    QrActionChip(
                        text = stringResource(R.string.note),
                        icon = TrailIcons.ToolNotes,
                        tag = "qr_save_note",
                        primary = true,
                        onClick = { onCreateNote(text) }
                    )
                }

                QrActionChip(
                    text = stringResource(android.R.string.copy),
                    icon = TrailIcons.Copy,
                    tag = "qr_copy",
                    onClick = { onCopy(text) }
                )
                QrActionChip(
                    text = stringResource(R.string.delete),
                    icon = TrailIcons.Delete,
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
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    TrailSenseButton(
        text = text,
        onClick = onClick,
        icon = icon,
        style = if (primary) TrailSenseButtonStyle.Filled else TrailSenseButtonStyle.Tonal,
        modifier = modifier.testTag(tag)
    )
}

private enum class ScanType {
    Text,
    Url,
    Geo
}

/** Each kind of code gets its own shape and tone, so links, places and notes tell apart at a glance. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private val ScanType.shape: androidx.graphics.shapes.RoundedPolygon
    get() = when (this) {
        ScanType.Text -> MaterialShapes.Cookie6Sided
        ScanType.Url -> MaterialShapes.Clover4Leaf
        ScanType.Geo -> MaterialShapes.Cookie9Sided
    }

@Composable
private fun ScanType.container(): Color = when (this) {
    ScanType.Text -> MaterialTheme.colorScheme.secondaryContainer
    ScanType.Url -> MaterialTheme.colorScheme.tertiaryContainer
    ScanType.Geo -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun ScanType.content(): Color = when (this) {
    ScanType.Text -> MaterialTheme.colorScheme.onSecondaryContainer
    ScanType.Url -> MaterialTheme.colorScheme.onTertiaryContainer
    ScanType.Geo -> MaterialTheme.colorScheme.onPrimaryContainer
}

/** Viewfinder corners in primary and a soft scan line sweeping down, over the live camera. */
@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val sweep by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "scanLine"
    )
    Canvas(modifier = modifier) {
        val inset = size.minDimension * 0.16f
        val arm = size.minDimension * 0.14f
        val stroke = 7.dp.toPx()
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        listOf(
            Offset(left, top) to Offset(1f, 1f),
            Offset(right, top) to Offset(-1f, 1f),
            Offset(left, bottom) to Offset(1f, -1f),
            Offset(right, bottom) to Offset(-1f, -1f)
        ).forEach { (corner, dir) ->
            drawLine(color, corner, corner + Offset(arm * dir.x, 0f), stroke, StrokeCap.Round)
            drawLine(color, corner, corner + Offset(0f, arm * dir.y), stroke, StrokeCap.Round)
        }
        val y = top + (bottom - top) * sweep
        drawLine(
            Brush.horizontalGradient(
                listOf(color.copy(alpha = 0f), color.copy(alpha = 0.8f), color.copy(alpha = 0f)),
                startX = left,
                endX = right
            ),
            Offset(left + stroke, y),
            Offset(right - stroke, y),
            3.dp.toPx(),
            StrokeCap.Round
        )
    }
}

private val ScanType.icon: ImageVector
    get() = when (this) {
        ScanType.Text -> TrailIcons.ToolNotes
        ScanType.Url -> TrailIcons.Link
        ScanType.Geo -> TrailIcons.Location
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
