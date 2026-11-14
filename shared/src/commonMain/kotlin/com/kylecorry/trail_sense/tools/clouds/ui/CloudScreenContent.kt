@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.images.rememberAppImage
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsText
import androidx.compose.ui.graphics.Color
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.time.toLocalDate
import com.kylecorry.trail_sense.shared.views.CloudsEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The cloud log: the last 48 hours of observations, newest first (null until read once, so "No clouds" never flashes
 * before it has), the cloud whose photo is open, and whether the scanner already opened for this visit.
 */
class CloudViewModel(
    private val repo: IReadingRepo<CloudObservation>,
    private val now: () -> Instant = { Clock.System.now() }
) : ViewModel() {

    val clouds: StateFlow<List<Reading<CloudObservation>>?> = repo.getAllLive()
        .map { all ->
            val since = now().minus(48.hours)
            all.sortedByDescending { cloud -> cloud.time }.filter { cloud -> cloud.time >= since }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var selectedCloudImage by mutableStateOf<CloudGenus?>(null)

    private var scannerOpened = false

    /** True only the first time, so the scanner shortcut opens the camera once, not on every return to the log. */
    fun consumeOpenScanner(): Boolean {
        if (scannerOpened) return false
        scannerOpened = true
        return true
    }

    fun delete(reading: Reading<CloudObservation>) {
        viewModelScope.launch { repo.delete(reading) }
    }
}
@Composable
fun CloudScreen(
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
    modifier: Modifier = Modifier,
    loaded: Boolean = true
) {
    var isCreateMenuOpen by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    val details = remember(strings) { CloudDetailsText(strings) }
    val formatter = remember { FormatService.getInstance() }

    selectedCloudImage?.let { cloud ->
        CloudImageDialog(
            cloud = cloud,
            onDismiss = onDismissCloudImage
        )
    }

    // The log covers two days, so "Today" and "Yesterday" are the natural groups.
    val days = remember(clouds) {
        clouds.groupBy { it.time.toLocalDate() }.toList()
    }

    // One question: what are the clouds saying? The latest cloud and what it may bring answer it; the log sits below.
    TrailSensePage(
        title = appString(Res.string.clouds),
        // "Last 48h" only means something once there is a log to scope.
        subtitle = subtitle.takeIf { it.isNotBlank() && !(loaded && clouds.isEmpty()) },
        backLabel = appString(Res.string.back),
        modifier = modifier.testTag("cloud_list_title"),
        actions = {
            // A bare "?" in onSurfaceVariant like the other bars' actions, not a filled disc louder than the page.
            IconButton(onClick = onGuideClick) {
                Icon(
                    imageVector = TrailIcons.HelpSimple,
                    contentDescription = appString(Res.string.help),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            // On an empty log the page's own button is the one way in, so the floating one waits for the first cloud.
            if (!(loaded && clouds.isEmpty())) {
                Box {
                    // The screen's one main action, in primary.
                    ExtendedFloatingActionButton(
                        onClick = { isCreateMenuOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_btn"),
                        icon = {
                            Icon(
                                imageVector = TrailIcons.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text(appString(Res.string.add)) }
                    )
                    CloudCreateMenu(
                        expanded = isCreateMenuOpen,
                        onDismiss = { isCreateMenuOpen = false },
                        onCameraClick = onCameraClick,
                        onFileClick = onFileClick,
                        onManualClick = onManualClick
                    )
                }
            }
        },
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.sm,
            bottom = 96.dp
        )
    ) {
        if (loaded && clouds.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.Cloud,
                    illustration = { CloudsEmptyIllustration() },
                    title = appString(Res.string.no_clouds),
                    description = appString(Res.string.clouds_empty_description),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("cloud_empty_text"),
                    action = {
                        Box {
                            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                                text = appString(Res.string.sky_clouds_log),
                                onClick = { isCreateMenuOpen = true },
                                icon = TrailIcons.Add,
                                modifier = Modifier.testTag("add_btn")
                            )
                            CloudCreateMenu(
                                expanded = isCreateMenuOpen,
                                onDismiss = { isCreateMenuOpen = false },
                                onCameraClick = onCameraClick,
                                onFileClick = onFileClick,
                                onManualClick = onManualClick
                            )
                        }
                    }
                )
            }
        }

        clouds.firstOrNull()?.let { latest ->
            item(key = "latest") {
                CloudSkyHero(
                    reading = latest,
                    onClick = { onCloudClick(latest) },
                    onImageClick = { onCloudImageClick(latest) }
                )
            }
        }

        days.forEachIndexed { index, (day, readings) ->
            item(key = day.toString()) {
                TrailSenseRowGroup(
                    modifier = Modifier
                        .animateItem()
                        .then(if (index == 0) Modifier.testTag("cloud_list") else Modifier),
                    title = formatter.formatRelativeDate(day, abbreviateMonth = true)
                ) {
                    readings.forEach { reading ->
                        row { position ->
                            key(reading.value.id) {
                            CloudReadingRow(
                                reading = reading,
                                position = position,
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
    }
}

/**
 * One logged cloud: its photo (tap to enlarge), name, then when and what it may mean, with delete in the overflow.
 */
@Composable
private fun CloudReadingRow(
    reading: Reading<CloudObservation>,
    position: TrailSenseGroupPosition,
    onCloudClick: (Reading<CloudObservation>) -> Unit,
    onCloudImageClick: (Reading<CloudObservation>) -> Unit,
    onDeleteCloud: (Reading<CloudObservation>) -> Unit
) {
    val strings = LocalAppStrings.current
    val details = remember(strings) { CloudDetailsText(strings) }
    val formatter = remember { FormatService.getInstance() }
    val cloud = reading.value.genus
    val name = details.getCloudName(cloud)
    var isMenuOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "cloudRowPress"
    )

    Surface(
        onClick = { onCloudClick(reading) },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = interactionSource
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
            CloudThumbnail(
                cloud = cloud,
                contentDescription = name,
                onClick = { onCloudImageClick(reading) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.md),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatter.formatTime(reading.time, includeSeconds = false) + " · " +
                        details.getCloudForecast(cloud),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.testTag("cloud_item_menu")
                ) {
                    Icon(
                        imageVector = TrailIcons.MenuDots,
                        contentDescription = appString(Res.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.delete)) },
                        leadingIcon = { Icon(TrailIcons.Delete, contentDescription = null) },
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

/**
 * The latest cloud on the plain page: its photo, its name in emphasised type, and what it may bring as the one verdict
 * line. Tap it for the details.
 */
@Composable
private fun CloudSkyHero(
    reading: Reading<CloudObservation>,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val details = remember(strings) { CloudDetailsText(strings) }
    val formatter = remember { FormatService.getInstance() }
    val cloud = reading.value.genus
    val name = details.getCloudName(cloud)
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
            .clickable(onClick = onClick)
            .padding(TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                details.getCloudForecast(cloud),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = TrailSenseSpacing.xxs)
            )
            Text(
                formatter.formatTime(reading.time, includeSeconds = false),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        CloudThumbnail(
            cloud = cloud,
            contentDescription = name,
            onClick = onImageClick,
            size = 88.dp
        )
    }
}

@Composable
private fun CloudThumbnail(
    cloud: CloudGenus?,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    shape: androidx.compose.ui.graphics.Shape = MaterialShapes.Cookie9Sided.toShape()
) {
    // A clear sky is drawn as the sky's colour; a cloud's photo once decoded.
    val strings = LocalAppStrings.current
    val bitmap = rememberAppImage(cloud?.let { CloudDetailsText(strings).getCloudImagePath(it) }, 240)
    val modifier = Modifier
        .size(size)
        .clip(shape)
        .background(if (cloud == null) Color(CloudDetailsText.CLEAR_SKY_COLOR) else MaterialTheme.colorScheme.secondaryContainer)
        .clickable(onClick = onClick)
        .semantics { this.contentDescription = contentDescription }
        .testTag("cloud_image")

    if (bitmap == null) {
        if (cloud == null) {
            Box(modifier)
            return
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = TrailIcons.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    } else {
        Image(
            bitmap = bitmap,
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

/** How to log a cloud: with the camera, from a photo, or by hand. */
@Composable
private fun CloudCreateMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onManualClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(appString(Res.string.camera)) },
            leadingIcon = { Icon(TrailIcons.Camera, contentDescription = null) },
            onClick = {
                onDismiss()
                onCameraClick()
            }
        )
        DropdownMenuItem(
            text = { Text(appString(Res.string.file)) },
            leadingIcon = { Icon(TrailIcons.File, contentDescription = null) },
            onClick = {
                onDismiss()
                onFileClick()
            }
        )
        DropdownMenuItem(
            text = { Text(appString(Res.string.manual)) },
            leadingIcon = { Icon(TrailIcons.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onManualClick()
            }
        )
    }
}
