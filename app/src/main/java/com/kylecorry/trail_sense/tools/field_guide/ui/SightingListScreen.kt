@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    SightingListScreen(
        title = controller.pageTitle,
        loaded = controller.loaded,
        items = controller.getSightingListItems(),
        onBack = { dispatcher?.onBackPressed() },
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
    var loaded by mutableStateOf(false)
        private set
    val pageTitle: String?
        get() = page?.name?.takeIf { it.isNotBlank() }

    fun loadPage() {
        host.lifecycleScope.launch {
            page = withContext(Dispatchers.IO) {
                repo.getPage(pageId)
            }
            loaded = true
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
    title: String?,
    loaded: Boolean,
    items: List<SightingListItem>,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val unknownLabel = stringResource(R.string.unknown)
    val sections = remember(items, unknownLabel) { groupSightingsByMonth(items, unknownLabel) }

    TrailSensePage(
        title = stringResource(R.string.sightings),
        subtitle = title,
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = Modifier.testTag("list"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.testTag("create_btn"),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.record_sighting)) }
            )
        }
    ) {
        if (!loaded) {
            return@TrailSensePage
        }

        if (items.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = Icons.Outlined.Pets,
                    title = stringResource(R.string.no_sightings),
                    description = stringResource(R.string.field_guide_no_sightings_description),
                    action = {
                        FilledTonalButton(onClick = onCreate) {
                            Text(stringResource(R.string.record_sighting))
                        }
                    }
                )
            }
            return@TrailSensePage
        }

        sections.forEach { section ->
            trailSenseSection(key = "month_${section.key}", title = section.title) {
                section.items.forEach { item ->
                    row { position ->
                        SightingRow(item = item, position = position)
                    }
                }
            }
        }
    }
}

private data class SightingSection(
    val key: String,
    val title: String,
    val items: List<SightingListItem>
)

/** Sightings fall into months, newest first, so a long history reads like a field journal. */
private fun groupSightingsByMonth(
    items: List<SightingListItem>,
    unknownLabel: String
): List<SightingSection> {
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
    return items
        .groupBy { item -> item.sighting.time?.let { YearMonth.from(it.toZonedDateTime()) } }
        .map { (month, sightings) ->
            SightingSection(
                key = month?.toString() ?: "unknown",
                title = month?.format(formatter)?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    ?: unknownLabel,
                items = sightings
            )
        }
}

@Composable
private fun SightingRow(
    item: SightingListItem,
    position: TrailSenseGroupPosition
) {
    var menuOpen by remember { mutableStateOf(false) }
    val harvested = stringResource(R.string.harvested)
    val subtitle = listOfNotNull(
        harvested.takeIf { item.isHarvested },
        item.subtitle.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
    ).joinToString(" · ")

    FieldGuideRow(
        title = AnnotatedString(item.title),
        subtitle = subtitle,
        position = position,
        leading = {
            FieldGuideIconWell(
                painterResource(if (item.hasLocation) R.drawable.ic_location else R.drawable.ic_date)
            )
        },
        onClick = item.onEdit,
        onLongClick = { menuOpen = true },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("field_guide_sighting_menu")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.field_guide_more_options),
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
    )
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
