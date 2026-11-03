@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Waves
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
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
        onCreate = controller::createTide,
        onSelect = controller::selectTide,
        onEdit = controller::editTide,
        onDelete = controller::deleteTide,
        onToggleVisibility = controller::toggleVisibility
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

@Composable
private fun TideListScreen(
    tides: List<Pair<TideTable, TideType?>>,
    onCreate: () -> Unit,
    onSelect: (TideTable) -> Unit,
    onEdit: (TideTable) -> Unit,
    onDelete: (TideTable) -> Unit,
    onToggleVisibility: (TideTable) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = TrailSenseSpacing.screen)
            ) {
                TideListHeader()
                TideTableList(
                    tides = tides,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onToggleVisibility = onToggleVisibility,
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
                text = { Text(stringResource(R.string.tides)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TrailSenseSpacing.lg)
                    .testTag("add_btn")
            )
        }
    }
}

@Composable
private fun TideListHeader() {
    Text(
        text = stringResource(R.string.tides),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = TrailSenseSpacing.xl,
                bottom = TrailSenseSpacing.sm
            )
            .testTag("tide_list_title"),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun TideTableList(
    tides: List<Pair<TideTable, TideType?>>,
    onSelect: (TideTable) -> Unit,
    onEdit: (TideTable) -> Unit,
    onDelete: (TideTable) -> Unit,
    onToggleVisibility: (TideTable) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.testTag("tide_list")) {
        if (tides.isEmpty()) {
            TrailSenseEmptyState(
                icon = Icons.Outlined.Waves,
                title = stringResource(R.string.no_tides),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tide_empty_text")
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = TrailSenseSpacing.sm,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                items(tides, key = { it.first.id }) { tide ->
                    TideTableRow(
                        tide = tide.first,
                        tideType = tide.second,
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

@Composable
private fun TideTableRow(
    tide: TideTable,
    tideType: TideType?,
    onSelect: (TideTable) -> Unit,
    onEdit: (TideTable) -> Unit,
    onDelete: (TideTable) -> Unit,
    onToggleVisibility: (TideTable) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    var isMenuOpen by remember { mutableStateOf(false) }

    TrailSenseCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (tide.isVisible) 1f else 0.55f)
            .testTag("tide_table_item"),
        onClick = { onSelect(tide) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tonal water/tide icon chip
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            getTideIcon(if (tide.isVisible) tideType else null)
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(TrailSenseSize.leadingIcon)
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getTideTitle(tide, formatter),
                    modifier = Modifier.testTag("tide_table_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(TrailSenseSpacing.xxs))
                Text(
                    text = getTideDescription(tide),
                    modifier = Modifier.testTag("tide_table_description"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (tide.isEditable) {
                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier
                            .size(TrailSenseSize.minimumTouchTarget)
                            .testTag("tide_table_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = getTideTitle(tide, formatter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                isMenuOpen = false
                                onEdit(tide)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (tide.isVisible) R.string.hide else R.string.show
                                    )
                                )
                            },
                            onClick = {
                                isMenuOpen = false
                                onToggleVisibility(tide)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                isMenuOpen = false
                                onDelete(tide)
                            }
                        )
                    }
                }
            }
        }
    }
}

@DrawableRes
private fun getTideIcon(tide: TideType?): Int {
    return when (tide) {
        TideType.High -> R.drawable.ic_tide_high
        TideType.Low -> R.drawable.ic_tide_low
        null -> R.drawable.ic_tide_half
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
            onCreate = {},
            onSelect = {},
            onEdit = {},
            onDelete = {},
            onToggleVisibility = {}
        )
    }
}
