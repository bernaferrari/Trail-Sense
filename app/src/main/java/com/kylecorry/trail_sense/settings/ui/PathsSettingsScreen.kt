@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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

    fun goBack() {
        host.goBack()
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
            prefs.paths.backtrackHistory.toDays().toInt(),
            allowDecimals = false,
            allowNegative = false,
            hint = host.getString(R.string.days)
        ) { days ->
            if (days != null) {
                val dayCount = days.toInt()
                prefs.paths.backtrackHistory = Duration.ofDays(days.toLong())
                onPicked(dayCount)
            }
        }
    }

    fun formatBacktrackInterval(): String {
        return formatService.formatDuration(
            prefs.paths.backtrackRecordFrequency,
            includeSeconds = true
        )
    }

    fun formatBacktrackHistory(): String {
        return formatService.formatDays(
            prefs.paths.backtrackHistory.toDays().toInt()
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

    private fun onBacktrackEnabled(_data: Bundle): Boolean {
        setBacktrackEnabledState?.invoke(true)
        return true
    }

    private fun onBacktrackDisabled(_data: Bundle): Boolean {
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
    val isBacktrackBlocked = remember { controller.isBacktrackBlocked() }

    var backtrackEnabled by remember { mutableStateOf(prefs.paths.backtrackEnabled) }
    var backtrackIntervalSummary by remember { mutableStateOf(controller.formatBacktrackInterval()) }
    var backtrackHistorySummary by remember { mutableStateOf(controller.formatBacktrackHistory()) }
    var backtrackNotificationsSummary by remember { mutableStateOf(controller.notificationSummary()) }
    var saveCellSignal by remember { mutableStateOf(prefs.paths.backtrackSaveCellHistory) }
    var showLastSignalBeacon by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_show_last_signal_beacon, true))
    }
    var pathStyle by remember {
        mutableStateOf(controller.getStringPreference(R.string.pref_backtrack_path_style, "dotted"))
    }
    var pathColor by remember { mutableStateOf(prefs.paths.defaultPathColor) }
    var simplifyPaths by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_auto_simplify_paths, true))
    }
    var fastPathRendering by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_fast_path_rendering, false))
    }
    var showPathColorPicker by remember { mutableStateOf(false) }
    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
    val pathStyleTitle = stringResource(R.string.pref_default_path_style_title)
    val pathColorTitle = stringResource(R.string.pref_default_path_color_title)
    val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)
    val backtrackTitle = stringResource(R.string.backtrack)
    val appearanceTitle = stringResource(R.string.appearance)

    controller.setBacktrackEnabledState { backtrackEnabled = it }
    controller.setRefreshNotificationState {
        backtrackNotificationsSummary = controller.notificationSummary()
    }

    TrailSensePage(
        title = stringResource(R.string.paths),
        onBack = controller::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("backtrack", title = backtrackTitle) {
            if (!isBacktrackBlocked) {
                row { position ->
                    SettingsSwitchRow(
                        label = backtrackTitle,
                        summary = stringResource(R.string.pref_backtrack_enabled_summary),
                        checked = backtrackEnabled,
                        onCheckedChange = {
                            backtrackEnabled = it
                            controller.toggleBacktrack { backtrackEnabled = false }
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = backtrackFrequencyTitle,
                    selected = backtrackIntervalSummary,
                    onClick = { showBacktrackFrequencyPicker = true },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_backtrack_history_days_title),
                    selected = backtrackHistorySummary,
                    onClick = {
                        controller.pickBacktrackHistory {
                            backtrackHistorySummary = formatService.formatDays(if (it > 0) it else 1)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = controller.notificationsTitle(),
                    selected = backtrackNotificationsSummary,
                    onClick = controller::openNotificationSettings,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_backtrack_save_cell_title),
                    summary = stringResource(R.string.pref_backtrack_save_cell_summary),
                    checked = saveCellSignal,
                    onCheckedChange = {
                        saveCellSignal = it
                        prefs.paths.backtrackSaveCellHistory = it
                    },
                    position = position
                )
            }
            if (saveCellSignal) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_last_signal_beacon_title),
                        summary = stringResource(R.string.pref_show_last_signal_beacon_summary),
                        checked = showLastSignalBeacon,
                        onCheckedChange = {
                            showLastSignalBeacon = it
                            controller.putBoolean(R.string.pref_show_last_signal_beacon, it)
                        },
                        position = position
                    )
                }
            }
        }

        trailSenseSection("appearance", title = appearanceTitle) {
            row { position ->
                SettingsPickerRow(
                    label = pathStyleTitle,
                    selected = controller.optionLabel(pathStyleOptions, pathStyle),
                    onClick = {
                        controller.pickOption(pathStyleTitle, pathStyleOptions, pathStyle) {
                            pathStyle = it
                            controller.putString(R.string.pref_backtrack_path_style, it)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = pathColorTitle,
                    onClick = { showPathColorPicker = true },
                    trailing = {
                        Box(
                            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(pathColor.color))
                            )
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_auto_simplify_paths_title),
                    checked = simplifyPaths,
                    onCheckedChange = {
                        simplifyPaths = it
                        controller.putBoolean(R.string.pref_auto_simplify_paths, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.fast_path_rendering),
                    summary = stringResource(R.string.fast_path_rendering_summary),
                    checked = fastPathRendering,
                    onCheckedChange = {
                        fastPathRendering = it
                        controller.putBoolean(R.string.pref_fast_path_rendering, it)
                    },
                    position = position
                )
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
                    prefs.paths.defaultPathColor = it
                }
                showPathColorPicker = false
            },
            onDismiss = { showPathColorPicker = false }
        )
    }

    if (showBacktrackFrequencyPicker) {
        DurationPickerDialog(
            title = backtrackFrequencyTitle,
            duration = prefs.paths.backtrackRecordFrequency,
            message = stringResource(R.string.actual_frequency_disclaimer),
            hint = stringResource(R.string.frequency),
            showSeconds = true,
            onDurationPicked = {
                if (it != null) {
                    controller.changeBacktrackFrequency(it) { frequency ->
                        backtrackIntervalSummary = formatService.formatDuration(frequency, includeSeconds = true)
                    }
                }
                showBacktrackFrequencyPicker = false
            },
            onDismiss = { showBacktrackFrequencyPicker = false }
        )
    }
}
