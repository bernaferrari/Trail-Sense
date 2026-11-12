@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import kotlinx.coroutines.IO
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.ToggleTideTableVisibilityCommand
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TideListToolScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel {
        val gps = getAppService<SensorService>().getGPS()
        TideListViewModel(
            tideRepo = getAppService<ITideTableRepo>(),
            tideTypeCommand = CurrentTideTypeCommand(TideService(getAppService<TideHarmonicsSource>())),
            toggleVisibilityCommand = ToggleTideTableVisibilityCommand(),
            tidePreferences = getAppService<UserPreferences>().tides,
            currentLocation = { gps.location }
        )
    }
    val formatService = remember { FormatService.getInstance() }
    val dispatcher = rememberSystemBack()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.refreshTides()
                Lifecycle.Event.ON_PAUSE -> viewModel.stop()
                Lifecycle.Event.ON_DESTROY -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    TideListScreen(
        tides = viewModel.tides,
        loaded = viewModel.loaded,
        onCreate = { navigator.navigate(Route.CreateTide()) },
        onSelect = {
            viewModel.select(it)
            navigator.navigateUp()
        },
        onEdit = { navigator.navigate(Route.CreateTide(editTideId = it.id)) },
        onDelete = { tide ->
            val title = tide.name ?: tide.location?.let {
                formatService.formatLocation(it)
            } ?: run {
                getAppService<AppStrings>().system(SystemString.Untitled)
            }
            Alerts.dialog(AppHost.current(),
                getAppService<AppStrings>().get(Res.string.delete_tide_prompt),
                title
            ) { cancelled ->
                if (!cancelled) {
                    viewModel.delete(tide)
                }
            }
        },
        onToggleVisibility = viewModel::toggleVisibility,
        onBack = { dispatcher?.invoke() }
    )
}

/**
 * The tide tables, built-in first and then nearest first, each with its current tide filled in as it is estimated.
 * Plain Kotlin over the tide table repository and preferences.
 */
internal class TideListViewModel(
    private val tideRepo: ITideTableRepo,
    private val tideTypeCommand: CurrentTideTypeCommand,
    private val toggleVisibilityCommand: ToggleTideTableVisibilityCommand,
    private val tidePreferences: TidePreferences,
    private val currentLocation: () -> Coordinate
) : ViewModel() {

    private var refreshJob: Job? = null
    var tides by mutableStateOf(emptyList<Pair<TideTable, TideType?>>())
        private set
    var loaded by mutableStateOf(false)
        private set

    fun stop() {
        refreshJob?.cancel()
    }

    fun delete(tide: TideTable) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                tideRepo.deleteTideTable(tide)
            }
            refreshTides()
        }
    }

    fun toggleVisibility(tide: TideTable) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                toggleVisibilityCommand.execute(tide)
            }
            refreshTides()
        }
    }

    fun select(tide: TideTable) {
        tidePreferences.lastTide = tide.id
    }

    fun refreshTides() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val location = currentLocation()
            val tables = withContext(Dispatchers.IO) {
                tideRepo.getTideTables().sortedWith(compareBy({ it.isEditable }, { tide ->
                    tide.location?.distanceTo(location) ?: Float.POSITIVE_INFINITY
                }))
            }
            tides = tables.map { it to null }
            loaded = true

            tables.forEach { table ->
                launch(Dispatchers.IO) {
                    val type = tideTypeCommand.execute(table)
                    withContext(Dispatchers.Main) {
                        val index = tides.indexOfFirst { it.first.id == table.id }
                        if (index != -1) {
                            tides = tides.toMutableList().also {
                                it[index] = table to type
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tide tables: the built-in model tables first, then the ones you entered, each a grouped row with the current tide
 * as its icon and a calm subtitle (how many tides, or the model). Tapping a table opens it; your own tables carry
 * edit, show/hide and delete in their overflow. A hidden table says so in its subtitle instead of fading out.
 */
@Composable
private fun TideListScreen(
    tides: List<Pair<TideTable, TideType?>>,
    loaded: Boolean,
    onCreate: () -> Unit,
    onSelect: (TideTable) -> Unit,
    onEdit: (TideTable) -> Unit,
    onDelete: (TideTable) -> Unit,
    onToggleVisibility: (TideTable) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val automatic = tides.filter { !it.first.isEditable }
    val entered = tides.filter { it.first.isEditable }

    TrailSensePage(
        title = appString(Res.string.tides),
        onBack = onBack,
        backLabel = appString(Res.string.back),
        modifier = Modifier.testTag("tide_list_title"),
        floatingActionButton = {
            if (loaded && tides.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(appString(Res.string.tide_table)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_btn")
            )
        }
    ) {
        if (!loaded) return@TrailSensePage

        if (tides.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.TideTable,
                    title = appString(Res.string.no_tides),
                    description = appString(Res.string.tides_empty_description),
                    actionLabel = appString(Res.string.tide_table),
                    actionIcon = TrailIcons.Add,
                    onAction = onCreate,
                    actionModifier = Modifier.testTag("add_btn"),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("tide_empty_text")
                )
            }
            return@TrailSensePage
        }

        item(key = "tables") {
            Column(
                modifier = Modifier.testTag("tide_list"),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                listOf(automatic, entered).filter { it.isNotEmpty() }.forEach { group ->
                    TrailSenseRowGroup {
                        group.forEach { (table, type) ->
                            row { position ->
                                TideTableRow(
                                    tide = table,
                                    tideType = type,
                                    position = position,
                                    onSelect = onSelect,
                                    onEdit = onEdit,
                                    onDelete = onDelete,
                                    onToggleVisibility = onToggleVisibility
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TideTableRow(
    tide: TideTable,
    tideType: TideType?,
    position: TrailSenseGroupPosition,
    onSelect: (TideTable) -> Unit,
    onEdit: (TideTable) -> Unit,
    onDelete: (TideTable) -> Unit,
    onToggleVisibility: (TideTable) -> Unit
) {
    val formatter = remember { FormatService.getInstance() }
    val description = getTideDescription(tide)
    val hidden = appString(Res.string.tides_hidden)

    TidesGroupRow(
        position = position,
        modifier = Modifier.testTag("tide_table_item"),
        onClick = { onSelect(tide) },
        leading = {
            val scheme = MaterialTheme.colorScheme
            val type = if (tide.isVisible) tideType else null
            PlacesBadge(
                containerColor = when (type) {
                    TideType.High -> scheme.primaryContainer
                    TideType.Low -> scheme.tertiaryContainer
                    null -> scheme.surfaceContainerHighest
                },
                contentColor = when (type) {
                    TideType.High -> scheme.onPrimaryContainer
                    TideType.Low -> scheme.onTertiaryContainer
                    null -> scheme.onSurfaceVariant
                },
                icon = getTideIcon(type),
                shape = if (tide.isEditable) MaterialShapes.Cookie6Sided.toShape() else MaterialShapes.Sunny.toShape(),
                modifier = Modifier.padding(start = TrailSenseSpacing.sm)
            )
        },
        trailing = if (tide.isEditable) {
            {
                TidesOverflowMenu(
                    contentDescription = appString(Res.string.tides_more_options),
                    modifier = Modifier.testTag("tide_table_menu"),
                    actions = listOf(
                        TidesMenuAction(appString(Res.string.edit)) { onEdit(tide) },
                        TidesMenuAction(
                            appString(if (tide.isVisible) Res.string.hide else Res.string.show)
                        ) { onToggleVisibility(tide) },
                        TidesMenuAction(appString(Res.string.delete)) { onDelete(tide) }
                    )
                )
            }
        } else {
            null
        }
    ) {
        Text(
            text = getTideTitle(tide, formatter),
            modifier = Modifier.testTag("tide_table_title"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (tide.isVisible) description else "$description · $hidden",
            modifier = Modifier.testTag("tide_table_description"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getTideIcon(tide: TideType?): ImageVector {
    return when (tide) {
        TideType.High -> TrailIcons.TideHigh
        TideType.Low -> TrailIcons.TideLow
        null -> TrailIcons.TideHalf
    }
}

@Composable
private fun getTideDescription(tide: TideTable): String {
    if (!tide.isEditable) {
        return appString(Res.string.tide_model_auto)
    }

    return getAppService<AppStrings>().plural(Res.plurals.tides_entered_count,
        tide.tides.size,
        tide.tides.size
    )
}

private fun getTideTitle(tide: TideTable, formatter: FormatService): String {
    return tide.name
        ?: tide.location?.let { formatter.formatLocation(it) }
        ?: ""
}

@Preview(showBackground = true)
@Composable
private fun TideListScreenPreview() {
    TrailSenseTheme {
        TideListScreen(
            tides = emptyList(),
            loaded = true,
            onCreate = {},
            onSelect = {},
            onEdit = {},
            onDelete = {},
            onToggleVisibility = {}
        )
    }
}
