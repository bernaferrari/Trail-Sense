package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class PathsSettingsFragment : TrailSenseComposeFragment() {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var setBacktrackEnabledState: ((Boolean) -> Unit)? = null
    private var refreshNotificationState: (() -> Unit)? = null

    @Composable
    override fun FragmentContent() {
                    val pathStyleOptions = remember {
                        options(R.array.line_style_entries, R.array.line_style_values)
                    }
                    val isBacktrackBlocked = remember {
                        Tools.getService(
                            requireContext(),
                            PathsToolRegistration.SERVICE_BACKTRACK
                        )?.isBlocked() ?: false
                    }

                    var backtrackEnabled by remember {
                        mutableStateOf(prefs.backtrackEnabled)
                    }
                    var backtrackIntervalSummary by remember {
                        mutableStateOf(formatBacktrackInterval())
                    }
                    var backtrackHistorySummary by remember {
                        mutableStateOf(formatBacktrackHistory())
                    }
                    var backtrackNotificationsSummary by remember {
                        mutableStateOf(notificationSummary())
                    }
                    var saveCellSignal by remember {
                        mutableStateOf(prefs.backtrackSaveCellHistory)
                    }
                    var showLastSignalBeacon by remember {
                        mutableStateOf(getBoolean(R.string.pref_show_last_signal_beacon, true))
                    }
                    var pathStyle by remember {
                        mutableStateOf(
                            getStringPreference(
                                R.string.pref_backtrack_path_style,
                                "dotted"
                            )
                        )
                    }
                    var pathColor by remember {
                        mutableStateOf(prefs.navigation.defaultPathColor)
                    }
                    var simplifyPaths by remember {
                        mutableStateOf(getBoolean(R.string.pref_auto_simplify_paths, true))
                    }
                    var onlyNavigateToPoints by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_only_navigate_path_points, true)
                        )
                    }
                    var fastPathRendering by remember {
                        mutableStateOf(getBoolean(R.string.pref_fast_path_rendering, false))
                    }
                    var showPathColorPicker by remember { mutableStateOf(false) }
                    val pathColorTitle = stringResource(R.string.pref_default_path_color_title)

                    setBacktrackEnabledState = {
                        backtrackEnabled = it
                    }
                    refreshNotificationState = {
                        backtrackNotificationsSummary = notificationSummary()
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
                                TrailSenseSectionHeader(title = stringResource(R.string.paths))
                            }
                            item("backtrack") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.backtrack),
                                    summary = stringResource(R.string.pref_backtrack_enabled_summary),
                                    checked = backtrackEnabled,
                                    enabled = !isBacktrackBlocked,
                                    onCheckedChange = {
                                        backtrackEnabled = it
                                        ToggleBacktrackCommand(this@PathsSettingsFragment) {
                                            backtrackEnabled = false
                                        }.execute()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("interval") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_backtrack_frequency_title),
                                    summary = backtrackIntervalSummary,
                                    onClick = {
                                        ChangeBacktrackFrequencyCommand(
                                            requireContext(),
                                            lifecycleScope
                                        ) {
                                            backtrackIntervalSummary =
                                                formatService.formatDuration(
                                                    it,
                                                    includeSeconds = true
                                                )
                                        }.execute()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("history") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_backtrack_history_days_title),
                                    summary = backtrackHistorySummary,
                                    onClick = {
                                        pickBacktrackHistory {
                                            backtrackHistorySummary =
                                                formatService.formatDays(
                                                    if (it > 0) it else 1
                                                )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("notifications") {
                                SettingsActionRow(
                                    label = getString(
                                        R.string.notifications_channel,
                                        getString(R.string.backtrack)
                                    ),
                                    summary = backtrackNotificationsSummary,
                                    onClick = {
                                        startActivity(
                                            Intents.notificationSettings(
                                                requireContext(),
                                                BacktrackService.FOREGROUND_CHANNEL_ID
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("save_cell") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_backtrack_save_cell_title),
                                    summary = stringResource(R.string.pref_backtrack_save_cell_summary),
                                    checked = saveCellSignal,
                                    onCheckedChange = {
                                        saveCellSignal = it
                                        prefs.backtrackSaveCellHistory = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("last_signal") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_show_last_signal_beacon_title),
                                    summary = stringResource(R.string.pref_show_last_signal_beacon_summary),
                                    checked = showLastSignalBeacon,
                                    enabled = saveCellSignal,
                                    onCheckedChange = {
                                        showLastSignalBeacon = it
                                        putBoolean(R.string.pref_show_last_signal_beacon, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("path_style") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_default_path_style_title),
                                    selected = optionLabel(pathStyleOptions, pathStyle),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_default_path_style_title),
                                            pathStyleOptions,
                                            pathStyle
                                        ) {
                                            pathStyle = it
                                            putString(R.string.pref_backtrack_path_style, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("path_color") {
                                SettingsActionRow(
                                    label = pathColorTitle,
                                    onClick = {
                                        showPathColorPicker = true
                                    },
                                    trailing = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(pathColor.color))
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("simplify") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_auto_simplify_paths_title),
                                    checked = simplifyPaths,
                                    onCheckedChange = {
                                        simplifyPaths = it
                                        putBoolean(R.string.pref_auto_simplify_paths, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("navigate_points") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_only_navigate_path_points_title),
                                    checked = onlyNavigateToPoints,
                                    onCheckedChange = {
                                        onlyNavigateToPoints = it
                                        putBoolean(R.string.pref_only_navigate_path_points, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("fast_rendering") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.fast_path_rendering),
                                    summary = stringResource(R.string.fast_path_rendering_summary),
                                    checked = fastPathRendering,
                                    onCheckedChange = {
                                        fastPathRendering = it
                                        putBoolean(R.string.pref_fast_path_rendering, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }

                    if (showPathColorPicker) {
                        ColorPickerDialog(
                            title = pathColorTitle,
                            selectedColor = pathColor,
                            onColorPicked = {
                                if (it != null) {
                                    pathColor = it
                                    prefs.navigation.defaultPathColor = it
                                }
                                showPathColorPicker = false
                            },
                            onDismiss = {
                                showPathColorPicker = false
                            }
                        )
                    }
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationState?.invoke()
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED,
            ::onBacktrackEnabled
        )
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED,
            ::onBacktrackDisabled
        )
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED,
            ::onBacktrackEnabled
        )
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED,
            ::onBacktrackDisabled
        )
    }

    override fun onDestroyView() {
        setBacktrackEnabledState = null
        refreshNotificationState = null
        super.onDestroyView()
    }

    private fun pickOption(
        title: String,
        options: List<PathOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun pickBacktrackHistory(onPicked: (Int) -> Unit) {
        Pickers.number(
            requireContext(),
            getString(R.string.pref_backtrack_history_days_title),
            null,
            prefs.navigation.backtrackHistory.toDays().toInt(),
            allowDecimals = false,
            allowNegative = false,
            hint = getString(R.string.days)
        ) { days ->
            if (days != null) {
                val dayCount = days.toInt()
                prefs.navigation.backtrackHistory = Duration.ofDays(days.toLong())
                onPicked(dayCount)
            }
        }
    }

    private fun formatBacktrackInterval(): String {
        return formatService.formatDuration(
            prefs.backtrackRecordFrequency,
            includeSeconds = true
        )
    }

    private fun formatBacktrackHistory(): String {
        return formatService.formatDays(
            prefs.navigation.backtrackHistory.toDays().toInt()
        )
    }

    private fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(
            requireContext(),
            BacktrackService.FOREGROUND_CHANNEL_ID
        )
        return if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    private fun options(entriesId: Int, valuesId: Int): List<PathOption> {
        val entries = resources.getStringArray(entriesId)
        val values = resources.getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            PathOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun optionLabel(options: List<PathOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
    }

    private fun getStringPreference(key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun onBacktrackEnabled(data: Bundle): Boolean {
        setBacktrackEnabledState?.invoke(true)
        return true
    }

    private fun onBacktrackDisabled(data: Bundle): Boolean {
        setBacktrackEnabledState?.invoke(false)
        return true
    }

    private data class PathOption(
        val value: String,
        val label: String
    )
}
