package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
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

class TideListFragment : TrailSenseComposeFragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val tideTypeCommand by lazy { CurrentTideTypeCommand(TideService(requireContext())) }

    private var refreshJob: Job? = null
    private var tides by mutableStateOf(emptyList<Pair<TideTable, TideType?>>())

    @Composable
    override fun FragmentContent() {
        TideListScreen(
            tides = tides,
            onCreate = ::createTide,
            onSelect = ::selectTide,
            onEdit = ::editTide,
            onDelete = ::deleteTide,
            onToggleVisibility = ::toggleVisibility
        )
    }

    override fun onResume() {
        super.onResume()
        refreshTides()
    }

    override fun onPause() {
        refreshJob?.cancel()
        super.onPause()
    }

    private fun deleteTide(tide: TideTable) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_tide_prompt),
            getTideTitle(tide)
        ) { cancelled ->
            if (!cancelled) {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        tideRepo.deleteTideTable(tide)
                    }
                    refreshTides()
                }
            }
        }
    }

    private fun toggleVisibility(tide: TideTable) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ToggleTideTableVisibilityCommand(requireContext()).execute(tide)
            }
            refreshTides()
        }
    }

    private fun editTide(tide: TideTable) {
        findNavController().navigate(
            R.id.action_tideList_to_createTide,
            Bundle().apply {
                putLong("edit_tide_id", tide.id)
            }
        )
    }

    private fun createTide() {
        findNavController().navigate(R.id.action_tideList_to_createTide)
    }

    private fun selectTide(tide: TideTable) {
        prefs.tides.lastTide = tide.id
        findNavController().navigateUp()
    }

    private fun getTideTitle(tide: TideTable): String {
        return tide.name
            ?: if (tide.location != null) formatService.formatLocation(tide.location) else getString(
                android.R.string.untitled
            )
    }

    private fun refreshTides() {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
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
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
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
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_btn")
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
private fun TideListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.tides),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("tide_list_title"),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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
            Text(
                text = stringResource(R.string.no_tides),
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("tide_empty_text"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
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
                    HorizontalDivider()
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(tide) }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
            .testTag("tide_table_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(getTideIcon(if (tide.isVisible) tideType else null)),
            contentDescription = null,
            tint = if (tide.isVisible) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                ComposeColor.Gray
            },
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.md)
        ) {
            Text(
                text = getTideTitle(tide, formatter),
                modifier = Modifier.testTag("tide_table_title"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    modifier = Modifier.testTag("tide_table_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null
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
