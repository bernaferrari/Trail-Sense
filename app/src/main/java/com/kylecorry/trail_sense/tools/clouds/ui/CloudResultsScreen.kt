@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toFile
import androidx.core.os.BundleCompat
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.clouds.domain.classification.ClassificationResult
import com.kylecorry.trail_sense.tools.clouds.domain.classification.ICloudClassifier
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.abs

@Composable
fun CloudResultsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CloudResultsController(host) }
    DisposableEffect(controller) {
        onDispose {
            controller.onDestroy()
        }
    }
    LaunchedEffect(controller) {
        controller.onResume()
    }

    CloudResultsScreen(
        image = controller.image,
        selection = controller.selection,
        title = stringResource(R.string.clouds),
        subtitle = controller.formatter.formatDateTime(
            controller.time.toZonedDateTime(),
            relative = true,
            abbreviateMonth = true
        ),
        isLoading = controller.isLoading,
        canSave = controller.selection.any { it.isSelected },
        onDateClick = controller::pickTime,
        onSave = controller::save,
        onSelectionChanged = controller::setSelection
    )
}

private class CloudResultsController(private val host: TrailSenseDestinationHost) {

    var image by mutableStateOf<Bitmap?>(null)
        private set
    private var classifier: ICloudClassifier = SoftmaxCloudClassifier(this::debugLogFeatures)
    var selection by mutableStateOf<List<CloudSelection>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var time by mutableStateOf(Instant.now())
        private set

    private val repo by lazy { CloudRepo.getInstance(host.requireContext()) }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }

    private val uri: Uri? by lazy {
        BundleCompat.getParcelable(host.destinationArguments ?: Bundle(), "image", Uri::class.java)
    }

    fun onResume() {
        if (uri != null && selection.isEmpty()) {
            analyze()
        } else if (selection.isEmpty()) {
            selection = CloudGenus.values().map { CloudSelection(it, null, false) } + listOf(
                CloudSelection(
                    null,
                    null,
                    false
                )
            )
        }
    }

    fun onDestroy() {
        image?.recycle()
        image = null
    }

    fun pickTime() {
        host.lifecycleScope.launch {
            val current = time.toZonedDateTime().toLocalDateTime()
            val newTime = CoroutinePickers.datetime(
                host.requireContext(),
                UserPreferences(host.requireContext()).use24HourTime,
                current
            )
            if (newTime != null) {
                time = newTime.toZonedDateTime().toInstant()
            }
        }
    }

    fun setSelection(genus: CloudGenus?, selected: Boolean) {
        selection = selection.map {
            if (genus == it.genus) {
                it.copy(isSelected = selected)
            } else {
                it
            }
        }
    }

    fun save() {
        host.lifecycleScope.launch {
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), time) }
            readings.forEach {
                repo.add(it)
            }
            host.findNavController().navigateUp()
        }
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(host.requireContext(), features).execute()
    }

    private fun analyze() {
        val uri = uri ?: return
        isLoading = true
        selection = emptyList()

        host.lifecycleScope.launch {
            if (image == null) {
                image = loadImage(uri)
                DeleteTempFilesCommand(host.requireContext()).execute()
            }
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }
            results?.let { setResult(it) } ?: run {
                isLoading = false
            }
        }
    }

    private suspend fun loadImage(uri: Uri): Bitmap? = onIO {
        val file = uri.toFile()
        val path = file.path
        val rotation = tryOrDefault(0) {
            val exif = ExifInterface(uri.toFile())
            exif.rotationDegrees
        }
        val full = BitmapUtils.decodeBitmapScaled(
            path,
            SoftmaxCloudClassifier.IMAGE_SIZE,
            SoftmaxCloudClassifier.IMAGE_SIZE
        ) ?: return@onIO null
        val bmp = full.resizeExact(
            SoftmaxCloudClassifier.IMAGE_SIZE,
            SoftmaxCloudClassifier.IMAGE_SIZE
        )
        full.recycle()
        val rotated = bmp.rotate(rotation.toFloat())
        bmp.recycle()
        rotated
    }

    private fun setResult(result: List<ClassificationResult<CloudGenus?>>) {
        isLoading = false
        val maxConfidence = result[0].confidence
        val threshold = if (result[0].value == null) 0.001f else 0.1f
        selection = result.map {
            CloudSelection(
                it.value,
                it.confidence,
                abs(it.confidence - maxConfidence) <= threshold
            )
        }
    }
}

@Composable
private fun CloudResultsScreen(
    image: Bitmap?,
    selection: List<CloudSelection>,
    title: String,
    subtitle: String,
    isLoading: Boolean,
    canSave: Boolean,
    onDateClick: () -> Unit,
    onSave: () -> Unit,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
        ) {
            CloudResultsHeader(
                title = title,
                subtitle = subtitle,
                canSave = canSave,
                onDateClick = onDateClick,
                onSave = onSave
            )
            Box(modifier = Modifier.fillMaxSize()) {
                CloudSelectionList(
                    image = image,
                    selection = selection,
                    onSelectionChanged = onSelectionChanged,
                    modifier = Modifier.fillMaxSize()
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("loading_indicator")
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun CloudResultsHeader(
    title: String,
    subtitle: String,
    canSave: Boolean,
    onDateClick: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.sm,
                    top = TrailSenseSpacing.md,
                    bottom = TrailSenseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.testTag("cloud_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.xxs)
                        .heightIn(min = TrailSenseSize.minimumTouchTarget)
                        .clickable(onClick = onDateClick)
                        .testTag("cloud_subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .alpha(if (canSave) 1f else 0.38f)
                    .testTag("cloud_save_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    }
}

@Composable
private fun CloudSelectionList(
    image: Bitmap?,
    selection: List<CloudSelection>,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    var selectedCloudImage by remember { mutableStateOf<CloudGenus?>(null) }

    selectedCloudImage?.let { cloud ->
        CloudImageDialog(
            title = details.getCloudName(cloud),
            drawable = details.getCloudImage(context, cloud),
            onDismiss = { selectedCloudImage = null }
        )
    }

    // The classifier returns candidates sorted by confidence, so the first entry is
    // the primary identification and gets the hero treatment.
    val primary = selection.firstOrNull()
    val others = if (selection.isEmpty()) emptyList() else selection.drop(1)

    LazyColumn(
        modifier = modifier.testTag("cloud_list"),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.screen,
            end = TrailSenseSpacing.screen,
            top = TrailSenseSpacing.lg,
            bottom = TrailSenseSpacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        if (primary != null) {
            item(key = primary.genus?.ordinal ?: -1) {
                CloudPrimaryHero(
                    image = image,
                    selection = primary,
                    onSelectionChanged = onSelectionChanged,
                    onCloudImageClick = { selectedCloudImage = it }
                )
            }
        }

        if (others.isNotEmpty()) {
            item(key = "other_candidates_header") {
                Text(
                    text = stringResource(R.string.other),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                )
            }
        }

        items(others, key = { it.genus?.ordinal ?: -1 }) { cloud ->
            CloudSelectionCard(
                selection = cloud,
                onSelectionChanged = onSelectionChanged,
                onCloudImageClick = { selectedCloudImage = it }
            )
        }
    }
}

/**
 * Hero card for the top identified cloud — tonal primaryContainer surface with the
 * captured photo, cloud name, description and confidence, plus the selection checkbox.
 */
@Composable
private fun CloudPrimaryHero(
    image: Bitmap?,
    selection: CloudSelection,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit,
    onCloudImageClick: (CloudGenus?) -> Unit
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val detailsModal = remember(context) { CloudDetailsModal(context) }
    val unreliable = remember { unreliableCloudGenera }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { detailsModal.show(selection.genus) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("cloud_image")
                    )
                } else {
                    CloudIconChip(
                        drawable = details.getCloudImage(context, selection.genus),
                        size = 72.dp,
                        contentDescription = details.getCloudName(selection.genus),
                        onClick = { onCloudImageClick(selection.genus) }
                    )
                }
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.getCloudName(selection.genus),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selection.confidence != null || unreliable.contains(selection.genus)) {
                        Row(
                            modifier = Modifier.padding(top = TrailSenseSpacing.xxs),
                            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                        ) {
                            selection.confidence?.let {
                                Text(
                                    text = formatter.formatPercentage(it * 100, 0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (unreliable.contains(selection.genus)) {
                                Text(
                                    text = stringResource(R.string.experimental),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                Checkbox(
                    checked = selection.isSelected,
                    onCheckedChange = { onSelectionChanged(selection.genus, it) },
                    modifier = Modifier.testTag("cloud_selection_checkbox")
                )
            }
            Text(
                text = details.getCloudDescription(selection.genus),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CloudSelectionCard(
    selection: CloudSelection,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit,
    onCloudImageClick: (CloudGenus?) -> Unit
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val detailsModal = remember(context) { CloudDetailsModal(context) }
    val unreliable = remember { unreliableCloudGenera }

    TrailSenseCard(onClick = { detailsModal.show(selection.genus) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selection.isSelected,
                onCheckedChange = { onSelectionChanged(selection.genus, it) },
                modifier = Modifier.testTag("cloud_selection_checkbox")
            )
            CloudIconChip(
                drawable = details.getCloudImage(context, selection.genus),
                size = 48.dp,
                contentDescription = details.getCloudName(selection.genus),
                onClick = { onCloudImageClick(selection.genus) }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.md)
            ) {
                Text(
                    text = details.getCloudName(selection.genus),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = details.getCloudDescription(selection.genus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (selection.confidence != null || unreliable.contains(selection.genus)) {
                    Row(
                        modifier = Modifier.padding(top = TrailSenseSpacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                    ) {
                        selection.confidence?.let {
                            Text(
                                text = formatter.formatPercentage(it * 100, 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (unreliable.contains(selection.genus)) {
                            Text(
                                text = stringResource(R.string.experimental),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Circular tonal chip holding the cloud thumbnail. Falls back to an empty tonal
 * surface when no drawable is available (e.g. the "unknown" cloud).
 */
@Composable
private fun CloudIconChip(
    drawable: Drawable?,
    size: Dp,
    contentDescription: String,
    onClick: () -> Unit
) {
    val px = with(LocalDensity.current) { size.toPx().toInt() }
    val bitmap = remember(drawable, px) { drawable?.toBitmap(px, px) }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    }
}

private val unreliableCloudGenera = listOf(
    CloudGenus.Cumulonimbus,
    CloudGenus.Altostratus,
    CloudGenus.Stratus,
    CloudGenus.Cirrostratus,
    null
)
