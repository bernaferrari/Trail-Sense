package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.ToggleTideTableVisibilityCommand
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import com.kylecorry.trail_sense.tools.tides.ui.mappers.TideTableAction
import com.kylecorry.trail_sense.tools.tides.ui.mappers.TideTableListItemMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TideListFragment : Fragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapper by lazy {
        TideTableListItemMapper(
            requireContext(),
            this::onTideTableAction
        )
    }
    private val tideTypeCommand by lazy { CurrentTideTypeCommand(TideService(requireContext())) }

    private var refreshJob: Job? = null
    private var tides by mutableStateOf(emptyList<Pair<TideTable, TideType?>>())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    TideListScreen(
                        tides = tides,
                        mapper = mapper,
                        onCreate = ::createTide
                    )
                }
            }
        }
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

    private fun onTideTableAction(tide: TideTable, action: TideTableAction) {
        when (action) {
            TideTableAction.Select -> selectTide(tide)
            TideTableAction.Edit -> editTide(tide)
            TideTableAction.Delete -> deleteTide(tide)
            TideTableAction.ToggleVisibility -> toggleVisibility(tide)
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
    mapper: TideTableListItemMapper,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TideListHeader()
                TideListView(
                    tides = tides,
                    mapper = mapper,
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
private fun TideListView(
    tides: List<Pair<TideTable, TideType?>>,
    mapper: TideTableListItemMapper,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("tide_list"),
        factory = { context ->
            FrameLayout(context).apply {
                val list = AndromedaListView(context, null).apply {
                    id = R.id.tide_list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val empty = TextView(context).apply {
                    id = View.generateViewId()
                    text = context.getString(R.string.no_tides)
                    gravity = Gravity.CENTER
                    visibility = View.GONE
                }
                list.emptyView = empty
                addView(list)
                addView(
                    empty,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }
        },
        update = { container ->
            container.findViewById<AndromedaListView>(R.id.tide_list)
                .setItems(tides, mapper)
        }
    )
}
