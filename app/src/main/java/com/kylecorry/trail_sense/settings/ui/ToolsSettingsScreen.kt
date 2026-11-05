package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme

@Composable
fun ToolsSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()) }
    val cache = remember { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val widgetThemeOptions = remember { host.widgetThemeOptions() }
    var widgetTheme by remember { mutableStateOf(host.getDefaultWidgetTheme(cache)) }
    val tools = remember {
        AlphabeticalToolSort().sort(Tools.getTools(host.requireContext())).first().tools
    }
    var bottomNavSelection by remember {
        mutableStateOf(prefs.bottomNavigationTools)
    }
    val bottomNavigationTitle = stringResource(R.string.bottom_navigation)
    // The last slot can't be changed, so it is said once below the slots instead of drawn as a dead row.
    val toolsSlotFooter = stringResource(
        R.string.settings_bottom_navigation_tools_slot,
        bottomNavSelection.size + 1
    )

    // Titled like the Settings row that opens it ("Tools" read as the tool list).
    SettingsList(host, title = stringResource(R.string.sky_settings_tools_summary)) {
        trailSenseSection("shortcuts") {
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.quick_actions),
                    summary = stringResource(R.string.tool_quick_action_summary),
                    onClick = { host.pickQuickActions(prefs) },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.widget_pref_title),
                    summary = stringResource(R.string.widget_bottom_sheet_instructions),
                    onClick = { host.pickWidgets(prefs) },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.default_widget_theme),
                    selected = optionLabel(widgetThemeOptions, widgetTheme),
                    onClick = {
                        host.pickOption(host.getString(R.string.default_widget_theme), widgetThemeOptions, widgetTheme) {
                            widgetTheme = it
                            host.putString(cache, R.string.pref_default_widget_theme, it)
                            Tools.triggerWidgetUpdate(host.requireContext(), null)
                        }
                    },
                    position = position
                )
            }
        }
        trailSenseSection(
            "bottom_navigation",
            title = bottomNavigationTitle,
            footer = toolsSlotFooter
        ) {
            bottomNavSelection.indices.forEach { index ->
                row { position ->
                    val pickerTitle = stringResource(R.string.bottom_navigation_slot, index + 1)
                    SettingsActionRow(
                        label = stringResource(R.string.settings_bottom_navigation_slot, index + 1),
                        summary = host.getBottomNavToolName(tools, bottomNavSelection.getOrNull(index)),
                        onClick = {
                            host.pickBottomNavTool(pickerTitle, index, tools, bottomNavSelection, prefs) {
                                bottomNavSelection = it
                            }
                        },
                        position = position
                    )
                }
            }
        }
    }
}

private fun TrailSenseDestinationHost.pickQuickActions(prefs: UserPreferences) {
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

private fun TrailSenseDestinationHost.pickWidgets(prefs: UserPreferences) {
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

private fun TrailSenseDestinationHost.pickBottomNavTool(
    title: String,
    index: Int,
    tools: List<Tool>,
    bottomNavSelection: List<Long>,
    prefs: UserPreferences,
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

private fun TrailSenseDestinationHost.getBottomNavToolName(tools: List<Tool>, id: Long?): String {
    return tools.firstOrNull { it.id == id }?.name ?: getString(R.string.none)
}

private fun TrailSenseDestinationHost.widgetThemeOptions(): List<SettingsOption> {
    return listOf(
        SettingsOption(
            WidgetTheme.System.id.toString(),
            getString(R.string.theme_system)
        ),
        SettingsOption(
            WidgetTheme.TransparentBlack.id.toString(),
            getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_black_text)
            )
        ),
        SettingsOption(
            WidgetTheme.TransparentWhite.id.toString(),
            getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_white_text)
            )
        )
    )
}

private fun TrailSenseDestinationHost.getDefaultWidgetTheme(cache: IPreferences): String {
    val key = getString(R.string.pref_default_widget_theme)
    val current = cache.getString(key)
    if (current != null) {
        return current
    }

    val default = WidgetTheme.System.id.toString()
    cache.putString(key, default)
    return default
}
