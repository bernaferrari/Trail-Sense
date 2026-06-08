@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
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
fun SightingListToolScreen(fragment: TrailSenseDestinationHost) {
    val controller = remember(fragment) { SightingListController(fragment) }
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

private class SightingListController(private val fragment: TrailSenseDestinationHost) {

    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val repo by lazy { FieldGuideRepo.getInstance(fragment.requireContext()) }
    private val navigator by lazy { Navigator.getInstance(fragment.requireContext()) }
    private val pageId by lazy {
        fragment.destinationArguments?.getLong("page_id") ?: error("Missing page id")
    }

    private var page by mutableStateOf<FieldGuidePage?>(null)

    fun loadPage() {
        fragment.lifecycleScope.launch {
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
                } ?: fragment.getString(R.string.sighting),
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
        fragment.findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
            }
        )
    }

    private fun editSighting(sighting: Sighting) {
        fragment.findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
                putLong("sighting_id", sighting.id)
            }
        )
    }

    private fun deleteSighting(sighting: Sighting) {
        Alerts.dialog(
            fragment.requireContext(),
            fragment.getString(R.string.delete),
            sighting.time?.let { time ->
                formatter.formatRelativeDateTime(
                    time.toZonedDateTime(),
                    includeSeconds = false
                )
            } ?: fragment.getString(R.string.sighting)
        ) { cancelled ->
            if (!cancelled) {
                fragment.lifecycleScope.launch {
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
        fragment.findNavController().openTool(Tools.NAVIGATION)
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
        fragment.findNavController().navigateWithAnimation(R.id.placeBeaconFragment, bundle)
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
            Column(modifier = Modifier.fillMaxSize()) {
                SightingListHeader()
                SightingListView(
                    items = items,
                    modifier = Modifier.fillMaxSize()
                )
            }
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("create_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SightingListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.sightings),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SightingListView(
    items: List<SightingListItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.testTag("list"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_sightings),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(modifier = modifier.testTag("list")) {
        items(items, key = { it.sighting.id }) { item ->
            SightingRow(item)
            TrailSenseDivider()
        }
    }
}

@Composable
private fun SightingRow(item: SightingListItem) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onEdit() }
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = TrailSenseSpacing.md)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.isHarvested) {
                Text(
                    text = stringResource(R.string.harvested),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("field_guide_sighting_menu")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_dots),
                    contentDescription = null
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
