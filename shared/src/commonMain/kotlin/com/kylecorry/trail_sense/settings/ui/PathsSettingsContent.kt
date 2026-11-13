@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.notifications.NotificationChannelControl
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionPrompt
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The paths settings page; [backtrackNotifications] is Backtrack's notification channel (its row hides when null). */
@Composable
fun PathsSettingsScreen(backtrackNotifications: NotificationChannelControl?) {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val cache = remember { getAppService<Preferences>() }
    val formatService = remember { FormatService.getInstance() }
    val prefs = remember { getAppService<UserPreferences>() }
    val pathStyleOptions = rememberSettingsOptions(Res.array.line_style_entries, Res.array.line_style_values)
    val isBacktrackBlocked = remember {
        getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK)?.isBlocked() ?: false
    }

    var backtrackEnabled by remember { mutableStateOf(prefs.paths.backtrackEnabled) }
    var backtrackIntervalSummary by remember {
        mutableStateOf(formatService.formatDuration(prefs.paths.backtrackRecordFrequency, includeSeconds = true))
    }
    var backtrackHistorySummary by remember {
        mutableStateOf(formatService.formatDays(prefs.paths.backtrackHistory.inWholeDays.toInt()))
    }
    var isNotificationBlocked by remember {
        mutableStateOf(backtrackNotifications?.isBlocked() ?: false)
    }
    var saveCellSignal by remember { mutableStateOf(prefs.paths.backtrackSaveCellHistory) }
    var showLastSignalBeacon by remember {
        mutableStateOf(cache.booleanOr(PreferenceKeys.showLastSignalBeacon, true))
    }
    var pathStyle by remember {
        mutableStateOf(cache.stringOr(PreferenceKeys.backtrackPathStyle, "dotted"))
    }
    var pathColor by remember { mutableStateOf(prefs.paths.defaultPathColor) }
    var simplifyPaths by remember {
        mutableStateOf(cache.booleanOr(PreferenceKeys.autoSimplifyPaths, true))
    }
    var fastPathRendering by remember {
        mutableStateOf(cache.booleanOr(PreferenceKeys.fastPathRendering, false))
    }
    var showPathColorPicker by remember { mutableStateOf(false) }
    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
    val pathStyleTitle = appString(Res.string.pref_default_path_style_title)
    val pathColorTitle = appString(Res.string.pref_default_path_color_title)
    val backtrackFrequencyTitle = appString(Res.string.pref_backtrack_frequency_title)
    val backtrackTitle = appString(Res.string.backtrack)
    val appearanceTitle = appString(Res.string.appearance)
    val backtrackNotificationsSummary =
        appString(if (isNotificationBlocked) Res.string.off else Res.string.on)

    // Backtrack can be turned on or off elsewhere while this page is shown; the channel may change in system settings.
    LifecycleResumeEffect(Unit) {
        isNotificationBlocked = backtrackNotifications?.isBlocked() ?: false
        val onEnabled: (EventData) -> Unit = { backtrackEnabled = true }
        val onDisabled: (EventData) -> Unit = { backtrackEnabled = false }
        ToolBroadcasts.subscribe(PathsBroadcasts.BACKTRACK_ENABLED, onEnabled)
        ToolBroadcasts.subscribe(PathsBroadcasts.BACKTRACK_DISABLED, onDisabled)
        onPauseOrDispose {
            ToolBroadcasts.unsubscribe(PathsBroadcasts.BACKTRACK_ENABLED, onEnabled)
            ToolBroadcasts.unsubscribe(PathsBroadcasts.BACKTRACK_DISABLED, onDisabled)
        }
    }

    TrailSensePage(
        title = appString(Res.string.paths),
        onBack = rememberGoBack(),
        backLabel = appString(Res.string.back)
    ) {
        trailSenseSection("backtrack", title = backtrackTitle) {
            if (!isBacktrackBlocked) {
                row { position ->
                    SettingsSwitchRow(
                        label = backtrackTitle,
                        summary = appString(Res.string.pref_backtrack_enabled_summary),
                        checked = backtrackEnabled,
                        onCheckedChange = {
                            backtrackEnabled = it
                            scope.launch {
                                toggleBacktrack(effects, scope) { backtrackEnabled = false }
                            }
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
                    label = appString(Res.string.pref_backtrack_history_days_title),
                    selected = backtrackHistorySummary,
                    onClick = {
                        pickBacktrackHistory(host, prefs) {
                            backtrackHistorySummary = formatService.formatDays(if (it > 0) it else 1)
                        }
                    },
                    position = position
                )
            }
            if (backtrackNotifications != null) {
                row { position ->
                    SettingsPickerRow(
                        label = appString(Res.string.notifications_channel, backtrackTitle),
                        selected = backtrackNotificationsSummary,
                        onClick = backtrackNotifications::openSettings,
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_backtrack_save_cell_title),
                    summary = appString(Res.string.pref_backtrack_save_cell_summary),
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
                        label = appString(Res.string.pref_show_last_signal_beacon_title),
                        summary = appString(Res.string.pref_show_last_signal_beacon_summary),
                        checked = showLastSignalBeacon,
                        onCheckedChange = {
                            showLastSignalBeacon = it
                            cache.putBoolean(PreferenceKeys.showLastSignalBeacon, it)
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
                    selected = optionLabel(pathStyleOptions, pathStyle),
                    onClick = {
                        pickOption(pathStyleTitle, pathStyleOptions, pathStyle) {
                            pathStyle = it
                            cache.putString(PreferenceKeys.backtrackPathStyle, it)
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
                    label = appString(Res.string.pref_auto_simplify_paths_title),
                    checked = simplifyPaths,
                    onCheckedChange = {
                        simplifyPaths = it
                        cache.putBoolean(PreferenceKeys.autoSimplifyPaths, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.fast_path_rendering),
                    summary = appString(Res.string.fast_path_rendering_summary),
                    checked = fastPathRendering,
                    onCheckedChange = {
                        fastPathRendering = it
                        cache.putBoolean(PreferenceKeys.fastPathRendering, it)
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
            message = appString(Res.string.actual_frequency_disclaimer),
            hint = appString(Res.string.frequency),
            showSeconds = true,
            onDurationPicked = {
                if (it != null) {
                    ChangeBacktrackFrequencyCommand(host, scope) { frequency ->
                        backtrackIntervalSummary = formatService.formatDuration(frequency, includeSeconds = true)
                    }.execute(it)
                }
                showBacktrackFrequencyPicker = false
            },
            onDismiss = { showBacktrackFrequencyPicker = false }
        )
    }
}

private fun pickBacktrackHistory(host: AppHost, prefs: UserPreferences, onPicked: (Int) -> Unit) {
    val strings = getAppService<AppStrings>()
    Pickers.number(
        host,
        strings.get(Res.string.pref_backtrack_history_days_title),
        null,
        prefs.paths.backtrackHistory.inWholeDays.toInt(),
        allowDecimals = false,
        allowNegative = false,
        hint = strings.get(Res.string.days)
    ) { days ->
        if (days != null) {
            val dayCount = days.toInt()
            prefs.paths.backtrackHistory = (days.toLong()).days
            onPicked(dayCount)
        }
    }
}

/**
 * Turns Backtrack on or off the way the Paths tool's toggle does: its permissions first, then the offer to lift
 * battery restrictions. [onPermissionDenied] runs when the permissions are refused.
 */
private suspend fun toggleBacktrack(
    effects: ScreenEffects,
    scope: CoroutineScope,
    onPermissionDenied: () -> Unit
) {
    val service = getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK) ?: return
    when (service.getFeatureState()) {
        FeatureState.On -> service.disable()
        FeatureState.Off -> {
            if (effects.requestPermission(AppPermission.Backtrack)) {
                service.enable()
                getAppService<BatteryRestrictionPrompt>().request(BatteryRestrictionsAlerter(effects, scope))
            } else {
                onPermissionDenied()
            }
        }

        FeatureState.Unavailable -> effects.showMessage(getAppService<AppStrings>().get(Res.string.backtrack_disabled_low_power_toast))
    }
}
