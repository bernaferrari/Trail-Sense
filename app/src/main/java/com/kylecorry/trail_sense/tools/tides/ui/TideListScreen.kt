@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.ToggleTideTableVisibilityCommand
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TideListToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { TideListController(host) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
        }
    }

    TideListScreen(
        tides = controller.tides,
        loaded = controller.loaded,
        onCreate = controller::createTide,
        onSelect = controller::selectTide,
        onEdit = controller::editTide,
        onDelete = controller::deleteTide,
        onToggleVisibility = controller::toggleVisibility,
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class TideListController(private val host: TrailSenseDestinationHost) {

    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val tideTypeCommand by lazy { CurrentTideTypeCommand(TideService(host.requireContext())) }

    private var refreshJob: Job? = null
    var tides by mutableStateOf(emptyList<Pair<TideTable, TideType?>>())
        private set
    var loaded by mutableStateOf(false)
        private set

    fun onResume() {
        refreshTides()
    }

    fun onPause() {
        refreshJob?.cancel()
    }

    fun deleteTide(tide: TideTable) {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete_tide_prompt),
            getTideTitle(tide)
        ) { cancelled ->
            if (!cancelled) {
                host.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        tideRepo.deleteTideTable(tide)
                    }
                    refreshTides()
                }
            }
        }
    }

    fun toggleVisibility(tide: TideTable) {
        host.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ToggleTideTableVisibilityCommand(host.requireContext()).execute(tide)
            }
            refreshTides()
        }
    }

    fun editTide(tide: TideTable) {
        host.findNavController().navigate(
            R.id.action_tideList_to_createTide,
            Bundle().apply {
                putLong("edit_tide_id", tide.id)
            }
        )
    }

    fun createTide() {
        host.findNavController().navigate(R.id.action_tideList_to_createTide)
    }

    fun selectTide(tide: TideTable) {
        prefs.tides.lastTide = tide.id
        host.findNavController().navigateUp()
    }

    private fun getTideTitle(tide: TideTable): String {
        return tide.name ?: if (tide.location != null) {
            formatService.formatLocation(tide.location)
        } else {
            host.getString(android.R.string.untitled)
        }
    }

    private fun refreshTides() {
        refreshJob?.cancel()
        refreshJob = host.lifecycleScope.launch {
            val tables = withContext(Dispatchers.IO) {
                tideRepo.getTideTables().sortedWith(compareBy({ it.isEditable }, { tide ->
                    tide.location?.distanceTo(gps.location) ?: Float.POSITIVE_INFINITY
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
        title = stringResource(R.string.tides),
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = Modifier.testTag("tide_list_title"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.tide_table)) },
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
                    title = stringResource(R.string.no_tides),
                    description = stringResource(R.string.tides_empty_description),
                    modifier = Modifier.testTag("tide_empty_text")
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
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val description = getTideDescription(tide)
    val hidden = stringResource(R.string.tides_hidden)

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
                    contentDescription = stringResource(R.string.tides_more_options),
                    modifier = Modifier.testTag("tide_table_menu"),
                    actions = listOf(
                        TidesMenuAction(stringResource(R.string.edit)) { onEdit(tide) },
                        TidesMenuAction(
                            stringResource(if (tide.isVisible) R.string.hide else R.string.show)
                        ) { onToggleVisibility(tide) },
                        TidesMenuAction(stringResource(R.string.delete)) { onDelete(tide) }
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
        return stringResource(R.string.tide_model_auto)
    }

    return LocalContext.current.resources.getQuantityString(
        R.plurals.tides_entered_count,
        tide.tides.size,
        tide.tides.size
    )
}

private fun getTideTitle(tide: TideTable, formatter: FormatService): String {
    return tide.name
        ?: if (tide.location != null) formatter.formatLocation(tide.location) else ""
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
