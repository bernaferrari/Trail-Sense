@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.core.net.toUri


import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
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
fun CloudResultsToolScreen(route: Route.CloudResults) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        CloudResultsViewModel(
            imageUri = route.imageUri,
            repo = CloudRepo.getInstance(appContext),
            classifier = SoftmaxCloudClassifier { DebugCloudCommand(appContext, it).execute() },
            loadImage = { loadCloudImage(it.toUri()) },
            clearTemp = { DeleteTempFilesCommand(appContext).execute() }
        )
    }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.navigateUp() }
    }

    CloudResultsScreen(
        image = viewModel.image,
        selection = viewModel.selection,
        title = stringResource(R.string.clouds),
        subtitle = formatter.formatDateTime(
            viewModel.time.toZonedDateTime(),
            relative = true,
            abbreviateMonth = true
        ),
        isLoading = viewModel.isLoading,
        canSave = viewModel.selection.any { it.isSelected },
        onDateClick = {
            scope.launch {
                val current = viewModel.time.toZonedDateTime().toLocalDateTime()
                val newTime = CoroutinePickers.datetime(
                    context,
                    UserPreferences(appContext).use24HourTime,
                    current
                )
                if (newTime != null) {
                    viewModel.time = newTime.toZonedDateTime().toInstant()
                }
            }
        },
        onSave = viewModel::save,
        onSelectionChanged = viewModel::setSelection
    )
}

/**
 * Picking the clouds in a photo (classified, best guesses preselected) or from every cloud type when there is no
 * photo, and the time they were seen. Reports [saved] once the chosen clouds are logged.
 */
internal class CloudResultsViewModel(
    private val imageUri: String?,
    private val repo: IReadingRepo<CloudObservation>,
    private val classifier: ICloudClassifier,
    private val loadImage: suspend (String) -> Bitmap?,
    private val clearTemp: suspend () -> Unit,
    now: Instant = Instant.now()
) : ViewModel() {

    var image by mutableStateOf<Bitmap?>(null)
        private set
    var selection by mutableStateOf<List<CloudSelection>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var time by mutableStateOf(now)

    private val _saved = Channel<Unit>(Channel.CONFLATED)
    /** One event per save, after the readings are stored. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (imageUri != null) {
            analyze(imageUri)
        } else {
            selection = CloudGenus.entries.map { CloudSelection(it, null, false) } + listOf(
                CloudSelection(
                    null,
                    null,
                    false
                )
            )
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
        viewModelScope.launch {
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), time) }
            readings.forEach {
                repo.add(it)
            }
            _saved.send(Unit)
        }
    }

    private fun analyze(uri: String) {
        isLoading = true
        selection = emptyList()

        viewModelScope.launch {
            if (image == null) {
                image = loadImage(uri)
                clearTemp()
            }
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }
            results?.let { setResult(it) } ?: run {
                isLoading = false
            }
        }
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

    override fun onCleared() {
        image?.recycle()
        image = null
    }
}

private suspend fun loadCloudImage(uri: Uri): Bitmap? = onIO {
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
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val detailsModal = remember(context) { CloudDetailsModal(context) }
    var selectedCloudImage by remember { mutableStateOf<CloudGenus?>(null) }

    selectedCloudImage?.let { cloud ->
        CloudImageDialog(
            title = details.getCloudName(cloud),
            drawable = details.getCloudImage(context, cloud),
            onDismiss = { selectedCloudImage = null }
        )
    }

    // A classified photo comes back sorted by confidence: the first candidate is the identification and the rest are
    // alternatives. A manual entry is one flat list of every cloud type.
    val isClassified = selection.any { it.confidence != null }
    val primary = if (isClassified) selection.take(1) else emptyList()
    val others = if (isClassified) selection.drop(1) else selection

    val otherLabel = stringResource(R.string.other)

    TrailSensePage(
        title = title,
        modifier = Modifier.testTag("cloud_title"),
        backLabel = stringResource(R.string.back),
        actions = {
            IconButton(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.testTag("cloud_save_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        if (image != null) {
            item(key = "photo") {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape.hero))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .testTag("cloud_image")
                )
            }
        }

        trailSenseSection(key = "time") {
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.time),
                    subtitle = subtitle,
                    icon = rememberVectorPainter(TrailIcons.Date),
                    onClick = onDateClick,
                    position = position,
                    modifier = Modifier.testTag("cloud_subtitle")
                )
            }
        }

        if (isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TrailSenseSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    TrailSenseLoadingIndicator(modifier = Modifier.testTag("loading_indicator"))
                }
            }
        }

        if (primary.isNotEmpty()) {
            trailSenseSection(key = "primary") {
                primary.forEach { cloud ->
                    row { position ->
                        CloudSelectionRow(
                            selection = cloud,
                            position = position,
                            showDescription = true,
                            onSelectionChanged = onSelectionChanged,
                            onCloudImageClick = { selectedCloudImage = it },
                            onDetailsClick = { detailsModal.show(it) }
                        )
                    }
                }
            }
        }

        if (others.isNotEmpty()) {
            trailSenseSection(
                key = "others",
                title = if (isClassified) otherLabel else null
            ) {
                others.forEach { cloud ->
                    row { position ->
                        CloudSelectionRow(
                            selection = cloud,
                            position = position,
                            showDescription = false,
                            onSelectionChanged = onSelectionChanged,
                            onCloudImageClick = { selectedCloudImage = it },
                            onDetailsClick = { detailsModal.show(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One candidate cloud. The whole row toggles it in or out of the observation (a chosen row takes the accent tone and a
 * check); the photo enlarges the reference picture and the info button opens the cloud's details.
 */
@Composable
private fun CloudSelectionRow(
    selection: CloudSelection,
    position: TrailSenseGroupPosition,
    showDescription: Boolean,
    onSelectionChanged: (CloudGenus?, Boolean) -> Unit,
    onCloudImageClick: (CloudGenus?) -> Unit,
    onDetailsClick: (CloudGenus?) -> Unit
) {
    val context = LocalContext.current
    val details = remember(context) { CloudDetailsService(context) }
    val formatter = remember(context) { FormatService.getInstance(context) }
    val name = details.getCloudName(selection.genus)
    val experimental = stringResource(R.string.experimental)
    val meta = listOfNotNull(
        selection.confidence?.let { formatter.formatPercentage(it * 100, 0) },
        experimental.takeIf { unreliableCloudGenera.contains(selection.genus) }
    ).joinToString(" · ")

    val container by animateColorAsState(
        targetValue = if (selection.isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "cloudSelectionContainer"
    )
    val contentColor = if (selection.isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (selection.isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trailSenseGroupShape(position))
            .toggleable(
                value = selection.isSelected,
                role = Role.Checkbox,
                onValueChange = { onSelectionChanged(selection.genus, it) }
            )
            .testTag("cloud_selection_checkbox"),
        shape = trailSenseGroupShape(position),
        color = container,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(
                    start = TrailSenseSpacing.md,
                    end = TrailSenseSpacing.xs,
                    top = TrailSenseSpacing.md,
                    bottom = TrailSenseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CloudIconChip(
                drawable = details.getCloudImage(context, selection.genus),
                size = if (showDescription) 88.dp else 52.dp,
                shape = if (showDescription) MaterialShapes.Puffy.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                isSelected = selection.isSelected,
                contentDescription = name,
                onClick = { onCloudImageClick(selection.genus) }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.md),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = name,
                    style = if (showDescription) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (showDescription) FontWeight.SemiBold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = supportingColor,
                        maxLines = 1
                    )
                }
                selection.confidence?.let { confidence ->
                    // How sure the match is, as a rounded bar (Keepout's mini bars).
                    val barHeight = if (showDescription) 10.dp else 6.dp
                    val fill by animateFloatAsState(
                        confidence.coerceIn(0f, 1f),
                        MaterialTheme.motionScheme.slowSpatialSpec(),
                        label = "cloudConfidence"
                    )
                    Box(
                        Modifier
                            .padding(vertical = TrailSenseSpacing.xs)
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fill)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(
                                    if (selection.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                        )
                    }
                }
                if (showDescription) {
                    Text(
                        text = details.getCloudDescription(selection.genus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = { onDetailsClick(selection.genus) }) {
                Icon(
                    imageVector = TrailIcons.Info,
                    contentDescription = stringResource(R.string.details),
                    tint = supportingColor
                )
            }
        }
    }
}

/**
 * The cloud's reference picture in a soft circle; when the cloud is chosen a check takes its place on top so the
 * choice reads at a glance. Falls back to a cloud glyph when there is no picture (the "unknown" cloud).
 */
@Composable
private fun CloudIconChip(
    drawable: Drawable?,
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val px = with(LocalDensity.current) { size.toPx().toInt() }
    val bitmap = remember(drawable, px) { drawable?.toBitmap(px, px) }
    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "cloudChipCheck"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = TrailIcons.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (checkAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { alpha = checkAlpha }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TrailIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
