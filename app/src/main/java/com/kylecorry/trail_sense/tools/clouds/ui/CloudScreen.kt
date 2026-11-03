@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Size
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.observe
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import java.time.Duration
import java.time.Instant

@Composable
fun CloudToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CloudController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    CloudScreen(
        subtitle = controller.subtitle,
        clouds = controller.clouds,
        onGuideClick = controller::showGuide,
        onCameraClick = controller::addFromCamera,
        onFileClick = controller::addFromFile,
        onManualClick = controller::addManual,
        onCloudClick = controller::showCloudDetails,
        onCloudImageClick = controller::showCloudImage,
        onDeleteCloud = controller::delete,
        selectedCloudImage = controller.selectedCloudImage,
        onDismissCloudImage = { controller.selectedCloudImage = null },
        modifier = Modifier.fillMaxSize()
    )
}

private class CloudController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { CloudRepo.getInstance(host.requireContext()) }
    private val cloudDetailsService by lazy { CloudDetailsService(host.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(host.requireContext()) }
    private var isInitialized = false

    var clouds by mutableStateOf(emptyList<Reading<CloudObservation>>())
        private set
    var subtitle by mutableStateOf("")
        private set
    var selectedCloudImage by mutableStateOf<CloudGenus?>(null)

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        subtitle = host.getString(
            R.string.last_duration, formatter.formatDuration(
                Duration.ofHours(48),
                short = true
            )
        )

        repo.getAllLive().observe(host.lifecycleOwner) {
            val since = Instant.now().minus(Duration.ofHours(48))
            clouds = it.sortedByDescending { cloud -> cloud.time }
                .filter { cloud -> cloud.time >= since }
        }

        val shouldOpenScanner = host.destinationArguments?.getBoolean("open_scanner", false) == true
        if (shouldOpenScanner) {
            addFromCamera()
        }
    }

    fun onDestroy() {
        selectedCloudImage = null
    }

    fun showGuide() {
        UserGuideUtils.showGuide(
            host.requireContext(),
            host.findNavController(),
            R.raw.guide_tool_clouds
        )
    }

    fun showCloudDetails(reading: Reading<CloudObservation>) {
        CloudDetailsModal(host.requireContext()).show(reading.value.genus)
    }

    fun showCloudImage(reading: Reading<CloudObservation>) {
        selectedCloudImage = reading.value.genus
    }

    fun addFromCamera() {
        host.requestCamera {
            if (it) {
                host.inBackground {
                    showPhotoImport(
                        Size(SoftmaxCloudClassifier.IMAGE_SIZE, SoftmaxCloudClassifier.IMAGE_SIZE)
                    )
                }
            } else {
                host.alertNoCameraPermission()
            }
        }
    }

    fun addFromFile() {
        host.inBackground(BackgroundMinimumState.Created) {
            val uri =
                IntentUriPicker(
                    host.intentResultRetriever,
                    host.requireContext()
                ).open(listOf("image/*")).getOrNull()
            val temp = uri?.let { onIO { files.copyToTemp(it) }?.toUri() }
            temp?.let { openCloudPicker(it) }
        }
    }

    private fun showPhotoImport(size: Size) {
        val prefs = UserPreferences(host.requireContext()).camera
        host.showPhotoImport(size, prefs.useZeroShutterLag) { uri ->
            uri?.let { openCloudPicker(it) }
        }
    }

    fun addManual() {
        host.findNavController().navigate(R.id.action_cloud_to_cloud_picker)
    }

    private fun openCloudPicker(uri: Uri) {
        host.findNavController().navigate(
            R.id.action_cloud_to_cloud_picker,
            Bundle().apply {
                putParcelable("image", uri)
            }
        )
    }

    fun delete(reading: Reading<CloudObservation>) {
        host.inBackground {
            val cancelled = onMain {
                CoroutineAlerts.dialog(
                    host.requireContext(),
                    host.getString(R.string.delete),
                    cloudDetailsService.getCloudName(reading.value.genus)
                )
            }
            if (!cancelled) {
                repo.delete(reading)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudScreen(
    subtitle: String,
    clouds: List<Reading<CloudObservation>>,
    onGuideClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onManualClick: () -> Unit,
    onCloudClick: (Reading<CloudObservation>) -> Unit,
    onCloudImageClick: (Reading<CloudObservation>) -> Unit,
    onDeleteCloud: (Reading<CloudObservation>) -> Unit,
    selectedCloudImage: CloudGenus?,
    onDismissCloudImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreateMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }

    selectedCloudImage?.let { cloud ->
        CloudImageDialog(
            title = details.getCloudName(cloud),
            drawable = details.getCloudImage(context, cloud),
            onDismiss = onDismissCloudImage
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.testTag("cloud_list_title")) {
                        Text(
                            text = stringResource(R.string.clouds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onGuideClick,
                        modifier = Modifier.size(TrailSenseSize.minimumTouchTarget)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help),
                            contentDescription = stringResource(R.string.help),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn"),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                DropdownMenu(
                    expanded = isCreateMenuOpen,
                    onDismissRequest = { isCreateMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.camera)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onCameraClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.file)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onFileClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.manual)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onManualClick()
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (clouds.isEmpty()) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Cloud,
                    title = stringResource(R.string.no_clouds),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("cloud_empty_text")
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = TrailSenseSize.maximumContentWidth)
                        .align(Alignment.TopCenter)
                        .testTag("cloud_list"),
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.screen,
                        top = TrailSenseSpacing.lg,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    items(clouds) { reading ->
                        CloudReadingListItem(
                            reading = reading,
                            onCloudClick = onCloudClick,
                            onCloudImageClick = onCloudImageClick,
                            onDeleteCloud = onDeleteCloud
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudReadingListItem(
    reading: Reading<CloudObservation>,
    onCloudClick: (Reading<CloudObservation>) -> Unit,
    onCloudImageClick: (Reading<CloudObservation>) -> Unit,
    onDeleteCloud: (Reading<CloudObservation>) -> Unit
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val cloud = reading.value.genus
    var isMenuOpen by remember { mutableStateOf(false) }

    TrailSenseCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCloudClick(reading) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CloudThumbnail(
                drawable = details.getCloudImage(context, cloud),
                contentDescription = details.getCloudName(cloud),
                onClick = { onCloudImageClick(reading) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.md)
            ) {
                Text(
                    text = details.getCloudName(cloud),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = details.getCloudForecast(cloud),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatter.formatDateTime(
                        reading.time.toZonedDateTime(),
                        relative = true,
                        abbreviateMonth = true
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier
                        .size(TrailSenseSize.minimumTouchTarget)
                        .testTag("cloud_item_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = details.getCloudName(cloud),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            isMenuOpen = false
                            onDeleteCloud(reading)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudThumbnail(
    drawable: Drawable?,
    contentDescription: String,
    onClick: () -> Unit
) {
    val bitmap = remember(drawable) {
        drawable?.toBitmap(112, 112)
    }
    val modifier = Modifier
        .size(56.dp)
        .clip(MaterialTheme.shapes.large)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .clickable(onClick = onClick)
        .semantics { this.contentDescription = contentDescription }
        .testTag("cloud_image")

    if (bitmap == null) {
        Box(modifier)
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CloudScreenPreview() {
    TrailSenseTheme {
        CloudScreen(
            subtitle = "Last 48h",
            clouds = emptyList(),
            onGuideClick = {},
            onCameraClick = {},
            onFileClick = {},
            onManualClick = {},
            onCloudClick = {},
            onCloudImageClick = {},
            onDeleteCloud = {},
            selectedCloudImage = null,
            onDismissCloudImage = {}
        )
    }
}
