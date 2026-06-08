package com.kylecorry.trail_sense.tools.tools.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort

class ToolsSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    @Composable
    override fun FragmentContent() {
                    val tools = remember {
                        AlphabeticalToolSort().sort(Tools.getTools(requireContext())).first().tools
                    }
                    var bottomNavSelection by remember {
                        mutableStateOf(prefs.bottomNavigationTools)
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.tools))
                            }
                            item("quick_actions") {
                                SettingsActionRow(
                                    label = stringResource(R.string.quick_actions),
                                    summary = stringResource(R.string.tool_quick_action_summary),
                                    onClick = ::pickQuickActions,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("widgets") {
                                SettingsActionRow(
                                    label = stringResource(R.string.widget_pref_title),
                                    summary = stringResource(R.string.widget_bottom_sheet_instructions),
                                    onClick = ::pickWidgets,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            items(bottomNavSelection.size) { index ->
                                val title =
                                    stringResource(R.string.bottom_navigation_slot, index + 1)
                                SettingsActionRow(
                                    label = title,
                                    summary = getBottomNavToolName(
                                        tools,
                                        bottomNavSelection.getOrNull(index)
                                    ),
                                    onClick = {
                                        pickBottomNavTool(
                                            title,
                                            index,
                                            tools,
                                            bottomNavSelection
                                        ) {
                                            bottomNavSelection = it
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("tools_fixed_slot") {
                                SettingsActionRow(
                                    label = stringResource(
                                        R.string.bottom_navigation_slot,
                                        bottomNavSelection.size + 1
                                    ),
                                    summary = stringResource(R.string.tools),
                                    enabled = false,
                                    onClick = {},
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                        }
                    }
    }

    private fun pickQuickActions() {
        val potentialActions = Tools.getQuickActions(requireContext())
            .filterNot { it.id == Tools.QUICK_ACTION_NONE }

        val selected = prefs.toolQuickActions

        val selectedIndices = potentialActions.mapIndexedNotNull { index, action ->
            if (selected.contains(action.id)) index else null
        }

        Pickers.items(
            requireContext(),
            getString(R.string.quick_actions),
            potentialActions.map { it.name },
            selectedIndices
        ) {
            if (it != null) {
                prefs.toolQuickActions = it.map { potentialActions[it].id }
            }
        }
    }

    private fun pickWidgets() {
        val tools = Tools.getTools(requireContext())
        val potentialWidgets = tools
            .flatMap { it.widgets }
            .filter { it.isEnabled(requireContext()) && it.canPlaceInApp }
            .distinctBy { it.id }
            .sortedBy { it.name }

        val selected = prefs.toolWidgets

        val selectedIndices = potentialWidgets.mapIndexedNotNull { index, widget ->
            if (selected.contains(widget.id)) index else null
        }

        Pickers.items(
            requireContext(),
            getString(R.string.widget_pref_title),
            potentialWidgets.map { it.name },
            selectedIndices
        ) {
            if (it != null) {
                prefs.toolWidgets = it.map { potentialWidgets[it].id }
            }
        }
    }

    private fun pickBottomNavTool(
        title: String,
        index: Int,
        tools: List<Tool>,
        bottomNavSelection: List<Long>,
        onSelectionChanged: (List<Long>) -> Unit
    ) {
        val names = listOf(getString(R.string.none)) + tools.map { it.name }
        val ids = listOf(0L) + tools.map { it.id }

        Pickers.item(
            requireContext(),
            title,
            names,
            ids.indexOfFirst { it == bottomNavSelection.getOrNull(index) }
        ) {
            if (it != null) {
                val selected = ids[it]
                val newSelection = bottomNavSelection.mapIndexed { selectionIndex, id ->
                    if (selectionIndex == index) selected else id
                }
                prefs.bottomNavigationTools = newSelection
                onSelectionChanged(newSelection)
                requireMainActivity().updateBottomNavigation()
            }
        }
    }

    private fun getBottomNavToolName(tools: List<Tool>, id: Long?): String {
        return tools.firstOrNull { it.id == id }?.name ?: getString(R.string.none)
    }
}
