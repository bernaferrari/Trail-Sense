@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toFile
import androidx.core.os.BundleCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
fun CloudResultsToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { CloudResultsController(fragment) }
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

private class CloudResultsController(private val fragment: Fragment) {

    var image by mutableStateOf<Bitmap?>(null)
        private set
    private var classifier: ICloudClassifier = SoftmaxCloudClassifier(this::debugLogFeatures)
    var selection by mutableStateOf<List<CloudSelection>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var time by mutableStateOf(Instant.now())
        private set

    private val repo by lazy { CloudRepo.getInstance(fragment.requireContext()) }
    val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }

    private val uri: Uri? by lazy {
        BundleCompat.getParcelable(fragment.arguments ?: Bundle(), "image", Uri::class.java)
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
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val current = time.toZonedDateTime().toLocalDateTime()
            val newTime = CoroutinePickers.datetime(
                fragment.requireContext(),
                UserPreferences(fragment.requireContext()).use24HourTime,
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
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), time) }
            readings.forEach {
                repo.add(it)
            }
            fragment.findNavController().navigateUp()
        }
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(fragment.requireContext(), features).execute()
    }

    private fun analyze() {
        val uri = uri ?: return
        isLoading = true
        selection = emptyList()

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            if (image == null) {
                image = loadImage(uri)
                DeleteTempFilesCommand(fragment.requireContext()).execute()
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
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CloudResultsHeader(
                title = title,
                subtitle = subtitle,
                canSave = canSave,
                onDateClick = onDateClick,
                onSave = onSave
            )
            if (image != null) {
                CloudImage(
                    image = image,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp)
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CloudSelectionList(
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
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
                        .padding(top = 2.dp)
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
                    .alpha(if (canSave) 1f else 0.38f)
                    .testTag("cloud_save_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CloudImage(
    image: Bitmap,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = image.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("cloud_image")
    )
}

@Composable
private fun CloudSelectionList(
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

    LazyColumn(
        modifier = modifier.testTag("cloud_list")
    ) {
        items(selection, key = { it.genus?.ordinal ?: -1 }) { cloud ->
            CloudSelectionRow(
                selection = cloud,
                onSelectionChanged = onSelectionChanged,
                onCloudImageClick = { selectedCloudImage = it }
            )
        }
    }
}

@Composable
private fun CloudSelectionRow(
    selection: CloudSelection,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit,
    onCloudImageClick: (CloudGenus?) -> Unit
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val detailsModal = remember(context) { CloudDetailsModal(context) }
    val unreliable = remember {
        listOf(
            CloudGenus.Cumulonimbus,
            CloudGenus.Altostratus,
            CloudGenus.Stratus,
            CloudGenus.Cirrostratus,
            null
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { detailsModal.show(selection.genus) }
            .padding(start = 8.dp, top = 10.dp, bottom = 10.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selection.isSelected,
            onCheckedChange = { onSelectionChanged(selection.genus, it) },
            modifier = Modifier.testTag("cloud_selection_checkbox")
        )
        CloudSelectionThumbnail(
            drawable = details.getCloudImage(context, selection.genus),
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
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        TrailSenseSpacing.md
                    )
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

@Composable
private fun CloudSelectionThumbnail(
    drawable: Drawable?,
    onClick: () -> Unit
) {
    val bitmap = remember(drawable) { drawable?.toBitmap(96, 96) }
    val modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick)

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
