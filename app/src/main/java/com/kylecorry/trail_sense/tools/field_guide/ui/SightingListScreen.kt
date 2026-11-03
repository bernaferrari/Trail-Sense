@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SightingListToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { SightingListController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.loadPage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        controller.loadPage()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SightingListScreen(
        items = controller.getSightingListItems(),
        onCreate = controller::createSighting
    )
}

private class SightingListController(private val host: TrailSenseDestinationHost) {

    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }
    private val pageId by lazy {
        host.destinationArguments?.getLong("page_id") ?: error("Missing page id")
    }

    private var page by mutableStateOf<FieldGuidePage?>(null)

    fun loadPage() {
        host.lifecycleScope.launch {
            page = withContext(Dispatchers.IO) {
                repo.getPage(pageId)
            }
        }
    }

    fun getSightingListItems(): List<SightingListItem> {
        val currentPage = page
        val sightings = currentPage?.sightings?.sortedByDescending { it.time } ?: emptyList()
        return sightings.map {
            SightingListItem(
                sighting = it,
                title = it.time?.let { time ->
                    formatter.formatRelativeDateTime(
                        time.toZonedDateTime(),
                        includeSeconds = false
                    )
                } ?: host.getString(R.string.sighting),
                subtitle = it.notes ?: "",
                isHarvested = it.harvested == true,
                hasLocation = it.location != null,
                onEdit = { editSighting(it) },
                onNavigate = { navigateToSighting(it) },
                onCreateBeacon = { createBeacon(it) },
                onDelete = { deleteSighting(it) }
            )
        }
    }

    fun createSighting() {
        host.findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
            }
        )
    }

    private fun editSighting(sighting: Sighting) {
        host.findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
                putLong("sighting_id", sighting.id)
            }
        )
    }

    private fun deleteSighting(sighting: Sighting) {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete),
            sighting.time?.let { time ->
                formatter.formatRelativeDateTime(
                    time.toZonedDateTime(),
                    includeSeconds = false
                )
            } ?: host.getString(R.string.sighting)
        ) { cancelled ->
            if (!cancelled) {
                host.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repo.deleteSighting(sighting)
                    }
                    loadPage()
                }
            }
        }
    }

    private fun navigateToSighting(sighting: Sighting) {
        navigator.navigateTo(
            sighting.location ?: Coordinate.zero,
            page?.name ?: "",
            BeaconOwner.FieldGuide,
            elevation = sighting.altitude
        )
        host.findNavController().openTool(Tools.NAVIGATION)
    }

    private fun createBeacon(sighting: Sighting) {
        val bundle = Bundle().apply {
            putParcelable(
                "initial_location",
                GeoUri(
                    sighting.location ?: Coordinate.zero,
                    sighting.altitude,
                    mapOf("label" to (page?.name ?: ""))
                )
            )
        }
        host.findNavController().navigateWithAnimation(R.id.placeBeaconFragment, bundle)
    }
}

@Composable
private fun SightingListScreen(
    items: List<SightingListItem>,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                SightingListHeader()
                SightingListView(
                    items = items,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ExtendedFloatingActionButton(
                onClick = onCreate,
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                },
                text = { Text(stringResource(R.string.sighting)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TrailSenseSpacing.screen)
                    .testTag("create_btn")
            )
        }
    }
}

@Composable
private fun SightingListHeader() {
    Text(
        text = stringResource(R.string.sightings),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = TrailSenseSpacing.screen,
                end = TrailSenseSpacing.screen,
                top = TrailSenseSpacing.xl,
                bottom = TrailSenseSpacing.sm
            ),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SightingListView(
    items: List<SightingListItem>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.testTag("list")) {
        if (items.isEmpty()) {
            TrailSenseEmptyState(
                icon = Icons.Outlined.Pets,
                title = stringResource(R.string.no_sightings),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.sm,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                items(items, key = { it.sighting.id }) { item ->
                    SightingRow(item)
                }
            }
        }
    }
}

@Composable
private fun SightingRow(item: SightingListItem) {
    var menuOpen by remember { mutableStateOf(false) }

    TrailSenseCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { item.onEdit() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_date),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.subtitle.isNotBlank()) {
                    Spacer(Modifier.size(TrailSenseSpacing.xxs))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.hasLocation || item.isHarvested) {
                    Spacer(Modifier.size(TrailSenseSpacing.sm))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                    ) {
                        if (item.hasLocation) {
                            SightingTag(
                                text = stringResource(R.string.location),
                                iconRes = R.drawable.ic_location
                            )
                        }
                        if (item.isHarvested) {
                            SightingTag(
                                text = stringResource(R.string.harvested)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("field_guide_sighting_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    if (item.hasLocation) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.navigate)) },
                            onClick = {
                                menuOpen = false
                                item.onNavigate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.create_beacon)) },
                            onClick = {
                                menuOpen = false
                                item.onCreateBeacon()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menuOpen = false
                            item.onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuOpen = false
                            item.onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SightingTag(
    text: String,
    iconRes: Int? = null
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.sm,
                vertical = TrailSenseSpacing.xxs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private data class SightingListItem(
    val sighting: Sighting,
    val title: String,
    val subtitle: String,
    val isHarvested: Boolean,
    val hasLocation: Boolean,
    val onEdit: () -> Unit,
    val onNavigate: () -> Unit,
    val onCreateBeacon: () -> Unit,
    val onDelete: () -> Unit
)
