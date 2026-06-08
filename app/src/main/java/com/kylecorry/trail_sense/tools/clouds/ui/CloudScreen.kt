@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.camera.PhotoImportDialog
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CloudToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { CloudController(fragment) }

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
    controller.PhotoImportContent()
}

private class CloudController(private val fragment: TrailSenseComposeDestinationFragment) {

    private val repo by lazy { CloudRepo.getInstance(fragment.requireContext()) }
    private val cloudDetailsService by lazy { CloudDetailsService(fragment.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(fragment.requireContext()) }
    private var isInitialized = false

    var clouds by mutableStateOf(emptyList<Reading<CloudObservation>>())
        private set
    var subtitle by mutableStateOf("")
        private set
    var selectedCloudImage by mutableStateOf<CloudGenus?>(null)
    private var photoImportDialog by mutableStateOf<PhotoImportDialog?>(null)

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        subtitle = fragment.getString(
            R.string.last_duration, formatter.formatDuration(
                Duration.ofHours(48),
                short = true
            )
        )

        fragment.observe(repo.getAllLive()) {
            val since = Instant.now().minus(Duration.ofHours(48))
            clouds = it.sortedByDescending { cloud -> cloud.time }
                .filter { cloud -> cloud.time >= since }
        }

        val shouldOpenScanner = fragment.requireArguments().getBoolean("open_scanner", false)
        if (shouldOpenScanner) {
            addFromCamera()
        }
    }

    fun onDestroy() {
        selectedCloudImage = null
        photoImportDialog?.dismiss()
        photoImportDialog = null
    }

    @Composable
    fun PhotoImportContent() {
        photoImportDialog?.Content()
    }

    fun showGuide() {
        UserGuideUtils.showGuide(
            fragment.requireContext(),
            fragment.findNavController(),
            R.raw.guide_tool_clouds
        )
    }

    fun showCloudDetails(reading: Reading<CloudObservation>) {
        CloudDetailsModal(fragment.requireContext()).show(reading.value.genus)
    }

    fun showCloudImage(reading: Reading<CloudObservation>) {
        selectedCloudImage = reading.value.genus
    }

    fun addFromCamera() {
        fragment.requestCamera {
            if (it) {
                fragment.inBackground {
                    val uri = takePhoto(
                        fragment,
                        Size(SoftmaxCloudClassifier.IMAGE_SIZE, SoftmaxCloudClassifier.IMAGE_SIZE)
                    )
                    uri?.let {
                        fragment.findNavController().navigate(
                            R.id.action_cloud_to_cloud_picker,
                            Bundle().apply {
                                putParcelable("image", uri)
                            }
                        )
                    }
                }
            } else {
                fragment.alertNoCameraPermission()
            }
        }
    }

    fun addFromFile() {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val uri =
                IntentUriPicker(
                    fragment.requireActivity() as IntentResultRetriever,
                    fragment.requireContext()
                ).open(listOf("image/*"))
            val temp = uri?.let { onIO { files.copyToTemp(it) }?.toUri() }
            temp?.let {
                fragment.findNavController().navigate(
                    R.id.action_cloud_to_cloud_picker,
                    Bundle().apply {
                        putParcelable("image", it)
                    }
                )
            }
        }
    }

    private suspend fun takePhoto(fragment: TrailSenseComposeDestinationFragment, size: Size): android.net.Uri? {
        return suspendCoroutine { cont ->
            val prefs = UserPreferences(fragment.requireContext()).camera
            fragment.requireActivity().runOnUiThread {
                photoImportDialog = PhotoImportDialog(
                    fragment.requireContext(),
                    fragment.viewLifecycleOwner,
                    fragment.viewLifecycleOwner.lifecycleScope,
                    size,
                    prefs.useZeroShutterLag
                ) { uri ->
                    photoImportDialog = null
                    cont.resume(uri)
                }.also {
                    it.show()
                }
            }
        }
    }

    fun addManual() {
        fragment.findNavController().navigate(R.id.action_cloud_to_cloud_picker)
    }

    fun delete(reading: Reading<CloudObservation>) {
        fragment.inBackground {
            val cancelled = onMain {
                CoroutineAlerts.dialog(
                    fragment.requireContext(),
                    fragment.getString(R.string.delete),
                    cloudDetailsService.getCloudName(reading.value.genus)
                )
            }
            if (!cancelled) {
                repo.delete(reading)
            }
        }
    }
}

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
            CloudHeader(
                subtitle = subtitle,
                onGuideClick = onGuideClick
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                }
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
                Text(
                    text = stringResource(R.string.no_clouds),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("cloud_empty_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cloud_list"),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(clouds) { reading ->
                        CloudReadingListItem(
                            reading = reading,
                            onCloudClick = onCloudClick,
                            onCloudImageClick = onCloudImageClick,
                            onDeleteCloud = onDeleteCloud
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudHeader(
    subtitle: String,
    onGuideClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("cloud_list_title")
            ) {
                Text(
                    text = stringResource(R.string.clouds),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onGuideClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = null
                )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCloudClick(reading) }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CloudThumbnail(
            drawable = details.getCloudImage(context, cloud),
            onClick = { onCloudImageClick(reading) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.md)
        ) {
            Text(
                text = details.getCloudName(cloud),
                style = MaterialTheme.typography.bodyLarge,
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
                modifier = Modifier.testTag("cloud_item_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
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

@Composable
private fun CloudThumbnail(
    drawable: Drawable?,
    onClick: () -> Unit
) {
    val bitmap = remember(drawable) {
        drawable?.toBitmap(112, 112)
    }
    val modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick)
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
