@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.views.compose.AppHost

import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator

import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.toRouteLocation
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.shared.time.displayName
import com.kylecorry.trail_sense.shared.time.toLocalDate
import kotlinx.datetime.yearMonth

@Composable
fun SightingListToolScreen(route: Route.SightingList) {
    val navigator = LocalNavigator.current
    val viewModel = viewModel { SightingListViewModel(route.pageId, getAppService<IFieldGuideRepo>()) }
    val formatter = remember { FormatService.getInstance() }
    val beaconNavigator = remember { getAppService<INavigator>() }
    val sightingLabel = appString(Res.string.sighting)

    LifecycleResumeEffect(viewModel) {
        viewModel.loadPage()
        onPauseOrDispose { }
    }

    fun sightingTitle(sighting: Sighting): String {
        return sighting.time?.let { time ->
            formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        } ?: sightingLabel
    }

    val page = viewModel.page
    val sightings = page?.sightings?.sortedByDescending { it.time } ?: emptyList()
    val items = sightings.map { sighting ->
        SightingListItem(
            sighting = sighting,
            title = sightingTitle(sighting),
            subtitle = sighting.notes ?: "",
            isHarvested = sighting.harvested == true,
            hasLocation = sighting.location != null,
            onEdit = {
                navigator.navigate(Route.CreateFieldGuideSighting(pageId = route.pageId, sightingId = sighting.id))
            },
            onNavigate = {
                beaconNavigator.navigateTo(
                    sighting.location ?: Coordinate.zero,
                    page?.name ?: "",
                    BeaconOwner.FieldGuide,
                    elevation = sighting.altitude
                )
                navigator.navigate(Route.Navigation())
            },
            onCreateBeacon = {
                val location = (sighting.location ?: Coordinate.zero).toRouteLocation(sighting.altitude)
                    .copy(parameters = mapOf("label" to (page?.name ?: "")))
                navigator.navigate(Route.PlaceBeacon(initialLocation = location))
            },
            onDelete = {
                Alerts.dialog(AppHost.current(),
                    getAppService<AppStrings>().get(Res.string.delete),
                    sightingTitle(sighting)
                ) { cancelled ->
                    if (!cancelled) {
                        viewModel.deleteSighting(sighting)
                    }
                }
            }
        )
    }

    val dispatcher = rememberSystemBack()
    SightingListScreen(
        title = viewModel.pageTitle,
        loaded = viewModel.loaded,
        items = items,
        onBack = { dispatcher?.invoke() },
        onCreate = { navigator.navigate(Route.CreateFieldGuideSighting(pageId = route.pageId)) }
    )
}

/** A field guide page's sightings, reread each time the list is shown and after a sighting is deleted. */
internal class SightingListViewModel(
    private val pageId: Long,
    private val repo: IFieldGuideRepo
) : ViewModel() {

    var page by mutableStateOf<FieldGuidePage?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set
    val pageTitle: String?
        get() = page?.name?.takeIf { it.isNotBlank() }

    fun loadPage() {
        viewModelScope.launch {
            page = withContext(ioDispatcher) {
                repo.getPage(pageId)
            }
            loaded = true
        }
    }

    fun deleteSighting(sighting: Sighting) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                repo.deleteSighting(sighting)
            }
            loadPage()
        }
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
    val unknownLabel = appString(Res.string.unknown)
    val sections = remember(items, unknownLabel) { groupSightingsByMonth(items, unknownLabel) }

    TrailSensePage(
        title = appString(Res.string.sightings),
        subtitle = title,
        onBack = onBack,
        backLabel = appString(Res.string.back),
        modifier = Modifier.testTag("list"),
        floatingActionButton = {
            // An empty list offers its one action in the empty state instead.
            if (loaded && items.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.testTag("create_btn"),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = { Text(appString(Res.string.record_sighting)) }
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
                    title = appString(Res.string.no_sightings),
                    description = appString(Res.string.field_guide_no_sightings_description),
                    actionLabel = appString(Res.string.record_sighting),
                    onAction = onCreate,
                    actionModifier = Modifier.testTag("create_btn"),
                    modifier = Modifier.fillParentMaxHeight(0.9f)
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
    return items
        .groupBy { item -> item.sighting.time?.toLocalDate()?.yearMonth }
        .map { (month, sightings) ->
            SightingSection(
                key = month?.toString() ?: "unknown",
                title = month?.let {
                    "${it.month.displayName(standalone = true)} ${it.year}"
                }?.replaceFirstChar { it.titlecase() }
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
    val harvested = appString(Res.string.harvested)
    val subtitle = listOfNotNull(
        harvested.takeIf { item.isHarvested },
        item.subtitle.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
    ).joinToString(" · ")

    FieldGuideRow(
        title = AnnotatedString(item.title),
        subtitle = subtitle,
        position = position,
        leading = {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(
                rememberVectorPainter(if (item.hasLocation) TrailIcons.Location else TrailIcons.Date)
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
                        contentDescription = appString(Res.string.field_guide_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    if (item.hasLocation) {
                        DropdownMenuItem(
                            text = { Text(appString(Res.string.navigate)) },
                            onClick = {
                                menuOpen = false
                                item.onNavigate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(appString(Res.string.create_beacon)) },
                            onClick = {
                                menuOpen = false
                                item.onCreateBeacon()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.edit)) },
                        onClick = {
                            menuOpen = false
                            item.onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.delete)) },
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
