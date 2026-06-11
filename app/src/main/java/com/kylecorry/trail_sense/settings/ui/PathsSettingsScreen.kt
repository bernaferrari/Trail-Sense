@file:Suppress("FunctionNaming")

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

@Composable
fun PathsSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PathsSettingsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    PathsSettingsContent(controller)
}

private class PathsSettingsController(private val host: TrailSenseDestinationHost) {

    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    val prefs by lazy { UserPreferences(host.requireContext()) }

    private var setBacktrackEnabledState: ((Boolean) -> Unit)? = null
    private var refreshNotificationState: (() -> Unit)? = null

    fun setBacktrackEnabledState(updater: (Boolean) -> Unit) {
        setBacktrackEnabledState = updater
    }

    fun setRefreshNotificationState(updater: () -> Unit) {
        refreshNotificationState = updater
    }

    fun onResume() {
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

    fun onPause() {
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED,
            ::onBacktrackEnabled
        )
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED,
            ::onBacktrackDisabled
        )
    }

    fun onDestroy() {
        setBacktrackEnabledState = null
        refreshNotificationState = null
    }

    fun isBacktrackBlocked(): Boolean {
        return Tools.getService(
            host.requireContext(),
            PathsToolRegistration.SERVICE_BACKTRACK
        )?.isBlocked() ?: false
    }

    fun toggleBacktrack(onBlocked: () -> Unit) {
        ToggleBacktrackCommand(host) {
            onBlocked()
        }.execute()
    }

    fun openNotificationSettings() {
        host.startActivity(
            Intents.notificationSettings(
                host.requireContext(),
                BacktrackService.FOREGROUND_CHANNEL_ID
            )
        )
    }

    fun changeBacktrackFrequency(
        duration: Duration,
        onChanged: (Duration) -> Unit
    ) {
        ChangeBacktrackFrequencyCommand(
            host.requireContext(),
            host.lifecycleScope
        ) { frequency ->
            onChanged(frequency)
        }.execute(duration)
    }

    fun pickOption(
        title: String,
        options: List<PathOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    fun pickBacktrackHistory(onPicked: (Int) -> Unit) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.pref_backtrack_history_days_title),
            null,
            prefs.navigation.backtrackHistory.toDays().toInt(),
            allowDecimals = false,
            allowNegative = false,
            hint = host.getString(R.string.days)
        ) { days ->
            if (days != null) {
                val dayCount = days.toInt()
                prefs.navigation.backtrackHistory = Duration.ofDays(days.toLong())
                onPicked(dayCount)
            }
        }
    }

    fun formatBacktrackInterval(): String {
        return formatService.formatDuration(
            prefs.backtrackRecordFrequency,
            includeSeconds = true
        )
    }

    fun formatBacktrackHistory(): String {
        return formatService.formatDays(
            prefs.navigation.backtrackHistory.toDays().toInt()
        )
    }

    fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(
            host.requireContext(),
            BacktrackService.FOREGROUND_CHANNEL_ID
        )
        return if (isBlocked) host.getString(R.string.off) else host.getString(R.string.on)
    }

    fun notificationsTitle(): String {
        return host.getString(
            R.string.notifications_channel,
            host.getString(R.string.backtrack)
        )
    }

    fun options(entriesId: Int, valuesId: Int): List<PathOption> {
        val entries = host.getResources().getStringArray(entriesId)
        val values = host.getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            PathOption(value, entries.getOrElse(index) { value })
        }
    }

    fun optionLabel(options: List<PathOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(host.getString(key)) ?: default
    }

    fun getStringPreference(key: Int, default: String): String {
        return cache.getString(host.getString(key)) ?: default
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(host.getString(key), value)
    }

    fun putString(key: Int, value: String) {
        cache.putString(host.getString(key), value)
    }

    private fun onBacktrackEnabled(data: Bundle): Boolean {
        setBacktrackEnabledState?.invoke(true)
        return true
    }

    private fun onBacktrackDisabled(data: Bundle): Boolean {
        setBacktrackEnabledState?.invoke(false)
        return true
    }
}

private data class PathOption(
    val value: String,
    val label: String
)

@Composable
private fun PathsSettingsContent(controller: PathsSettingsController) {
                    val prefs = controller.prefs
                    val formatService = controller.formatService
                    val pathStyleOptions = remember {
                        controller.options(R.array.line_style_entries, R.array.line_style_values)
                    }
                    val isBacktrackBlocked = remember {
                        controller.isBacktrackBlocked()
                    }

                    var backtrackEnabled by remember {
                        mutableStateOf(prefs.backtrackEnabled)
                    }
                    var backtrackIntervalSummary by remember {
                        mutableStateOf(controller.formatBacktrackInterval())
                    }
                    var backtrackHistorySummary by remember {
                        mutableStateOf(controller.formatBacktrackHistory())
                    }
                    var backtrackNotificationsSummary by remember {
                        mutableStateOf(controller.notificationSummary())
                    }
                    var saveCellSignal by remember {
                        mutableStateOf(prefs.backtrackSaveCellHistory)
                    }
                    var showLastSignalBeacon by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_show_last_signal_beacon, true))
                    }
                    var pathStyle by remember {
                        mutableStateOf(
                            controller.getStringPreference(
                                R.string.pref_backtrack_path_style,
                                "dotted"
                            )
                        )
                    }
                    var pathColor by remember {
                        mutableStateOf(prefs.navigation.defaultPathColor)
                    }
                    var simplifyPaths by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_auto_simplify_paths, true))
                    }
                    var onlyNavigateToPoints by remember {
                        mutableStateOf(
                            controller.getBoolean(R.string.pref_only_navigate_path_points, true)
                        )
                    }
                    var fastPathRendering by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_fast_path_rendering, false))
                    }
                    var showPathColorPicker by remember { mutableStateOf(false) }
                    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
                    val pathStyleTitle = stringResource(R.string.pref_default_path_style_title)
                    val pathColorTitle = stringResource(R.string.pref_default_path_color_title)
                    val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)

                    controller.setBacktrackEnabledState {
                        backtrackEnabled = it
                    }
                    controller.setRefreshNotificationState {
                        backtrackNotificationsSummary = controller.notificationSummary()
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
                                        controller.toggleBacktrack {
                                            backtrackEnabled = false
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("interval") {
                                SettingsActionRow(
                                    label = backtrackFrequencyTitle,
                                    summary = backtrackIntervalSummary,
                                    onClick = {
                                        showBacktrackFrequencyPicker = true
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("history") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_backtrack_history_days_title),
                                    summary = backtrackHistorySummary,
                                    onClick = {
                                        controller.pickBacktrackHistory {
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
                                    label = controller.notificationsTitle(),
                                    summary = backtrackNotificationsSummary,
                                    onClick = controller::openNotificationSettings,
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
                                        controller.putBoolean(R.string.pref_show_last_signal_beacon, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("path_style") {
                                SettingsPickerRow(
                                    label = pathStyleTitle,
                                    selected = controller.optionLabel(pathStyleOptions, pathStyle),
                                    onClick = {
                                        controller.pickOption(
                                            pathStyleTitle,
                                            pathStyleOptions,
                                            pathStyle
                                        ) {
                                            pathStyle = it
                                            controller.putString(R.string.pref_backtrack_path_style, it)
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
                                        controller.putBoolean(R.string.pref_auto_simplify_paths, it)
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
                                        controller.putBoolean(R.string.pref_only_navigate_path_points, it)
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
                                        controller.putBoolean(R.string.pref_fast_path_rendering, it)
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

                    if (showBacktrackFrequencyPicker) {
                        DurationPickerDialog(
                            title = backtrackFrequencyTitle,
                            duration = prefs.backtrackRecordFrequency,
                            message = stringResource(R.string.actual_frequency_disclaimer),
                            hint = stringResource(R.string.frequency),
                            showSeconds = true,
                            onDurationPicked = {
                                if (it != null) {
                                    controller.changeBacktrackFrequency(it) { frequency ->
                                        backtrackIntervalSummary =
                                            formatService.formatDuration(
                                                frequency,
                                                includeSeconds = true
                                            )
                                    }
                                }
                                showBacktrackFrequencyPicker = false
                            },
                            onDismiss = {
                                showBacktrackFrequencyPicker = false
                            }
                        )
                    }
}
