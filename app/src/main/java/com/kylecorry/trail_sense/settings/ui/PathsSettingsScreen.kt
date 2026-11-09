@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PathsSettingsToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val cache = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    val formatService = remember(context) { FormatService.getInstance(context) }
    val prefs = remember(context) { UserPreferences(context) }
    val pathStyleOptions = rememberSettingsOptions(R.array.line_style_entries, R.array.line_style_values)
    val isBacktrackBlocked = remember {
        Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK)?.isBlocked() ?: false
    }

    var backtrackEnabled by remember { mutableStateOf(prefs.paths.backtrackEnabled) }
    var backtrackIntervalSummary by remember {
        mutableStateOf(formatService.formatDuration(prefs.paths.backtrackRecordFrequency, includeSeconds = true))
    }
    var backtrackHistorySummary by remember {
        mutableStateOf(formatService.formatDays(prefs.paths.backtrackHistory.toDays().toInt()))
    }
    var isNotificationBlocked by remember {
        mutableStateOf(Notify.isChannelBlocked(context, BacktrackService.FOREGROUND_CHANNEL_ID))
    }
    var saveCellSignal by remember { mutableStateOf(prefs.paths.backtrackSaveCellHistory) }
    var showLastSignalBeacon by remember {
        mutableStateOf(context.getBoolean(cache, R.string.pref_show_last_signal_beacon, true))
    }
    var pathStyle by remember {
        mutableStateOf(context.getStringPreference(cache, R.string.pref_backtrack_path_style, "dotted"))
    }
    var pathColor by remember { mutableStateOf(prefs.paths.defaultPathColor) }
    var simplifyPaths by remember {
        mutableStateOf(context.getBoolean(cache, R.string.pref_auto_simplify_paths, true))
    }
    var fastPathRendering by remember {
        mutableStateOf(context.getBoolean(cache, R.string.pref_fast_path_rendering, false))
    }
    var showPathColorPicker by remember { mutableStateOf(false) }
    var showBacktrackFrequencyPicker by remember { mutableStateOf(false) }
    val pathStyleTitle = stringResource(R.string.pref_default_path_style_title)
    val pathColorTitle = stringResource(R.string.pref_default_path_color_title)
    val backtrackFrequencyTitle = stringResource(R.string.pref_backtrack_frequency_title)
    val backtrackTitle = stringResource(R.string.backtrack)
    val appearanceTitle = stringResource(R.string.appearance)
    val backtrackNotificationsSummary =
        stringResource(if (isNotificationBlocked) R.string.off else R.string.on)

    // Backtrack can be turned on or off elsewhere while this page is shown; the channel may change in system settings.
    LifecycleResumeEffect(Unit) {
        isNotificationBlocked = Notify.isChannelBlocked(context, BacktrackService.FOREGROUND_CHANNEL_ID)
        val onEnabled: (Bundle) -> Unit = { backtrackEnabled = true }
        val onDisabled: (Bundle) -> Unit = { backtrackEnabled = false }
        Tools.subscribe(PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED, onEnabled)
        Tools.subscribe(PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED, onDisabled)
        onPauseOrDispose {
            Tools.unsubscribe(PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED, onEnabled)
            Tools.unsubscribe(PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED, onDisabled)
        }
    }

    TrailSensePage(
        title = stringResource(R.string.paths),
        onBack = rememberGoBack(),
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
                            scope.launch {
                                toggleBacktrack(context, effects, scope) { backtrackEnabled = false }
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
                    label = stringResource(R.string.pref_backtrack_history_days_title),
                    selected = backtrackHistorySummary,
                    onClick = {
                        pickBacktrackHistory(context, prefs) {
                            backtrackHistorySummary = formatService.formatDays(if (it > 0) it else 1)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.notifications_channel, backtrackTitle),
                    selected = backtrackNotificationsSummary,
                    onClick = {
                        context.startActivity(
                            Intents.notificationSettings(context, BacktrackService.FOREGROUND_CHANNEL_ID)
                        )
                    },
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
                            context.putBoolean(cache, R.string.pref_show_last_signal_beacon, it)
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
                        context.pickOption(pathStyleTitle, pathStyleOptions, pathStyle) {
                            pathStyle = it
                            context.putString(cache, R.string.pref_backtrack_path_style, it)
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
                        context.putBoolean(cache, R.string.pref_auto_simplify_paths, it)
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
                        context.putBoolean(cache, R.string.pref_fast_path_rendering, it)
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
                    ChangeBacktrackFrequencyCommand(context, scope) { frequency ->
                        backtrackIntervalSummary = formatService.formatDuration(frequency, includeSeconds = true)
                    }.execute(it)
                }
                showBacktrackFrequencyPicker = false
            },
            onDismiss = { showBacktrackFrequencyPicker = false }
        )
    }
}

private fun pickBacktrackHistory(context: Context, prefs: UserPreferences, onPicked: (Int) -> Unit) {
    Pickers.number(
        context,
        context.getString(R.string.pref_backtrack_history_days_title),
        null,
        prefs.paths.backtrackHistory.toDays().toInt(),
        allowDecimals = false,
        allowNegative = false,
        hint = context.getString(R.string.days)
    ) { days ->
        if (days != null) {
            val dayCount = days.toInt()
            prefs.paths.backtrackHistory = Duration.ofDays(days.toLong())
            onPicked(dayCount)
        }
    }
}

/**
 * Turns Backtrack on or off the way the Paths tool's toggle does: its permissions first, then the offer to lift
 * battery restrictions. [onPermissionDenied] runs when the permissions are refused.
 */
private suspend fun toggleBacktrack(
    context: Context,
    effects: ScreenEffects,
    scope: CoroutineScope,
    onPermissionDenied: () -> Unit
) {
    val service = Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK) ?: return
    when (service.getFeatureState()) {
        FeatureState.On -> service.disable()
        FeatureState.Off -> {
            if (effects.requestPermission(AppPermission.Backtrack)) {
                service.enable()
                RequestRemoveBatteryRestrictionCommand(
                    context,
                    BatteryRestrictionsAlerter(context, effects, scope)
                ).execute()
            } else {
                onPermissionDenied()
            }
        }

        FeatureState.Unavailable -> effects.showMessage(context.getString(R.string.backtrack_disabled_low_power_toast))
    }
}

/** The battery restrictions snackbar over [ScreenEffects]; Learn more asks to ignore battery optimizations. */
private class BatteryRestrictionsAlerter(
    private val context: Context,
    private val effects: ScreenEffects,
    private val scope: CoroutineScope
) : IAlerter {
    override fun alert() {
        scope.launch {
            val learnMore = effects.showSnackbar(
                context.getString(R.string.battery_settings_limit_accuracy),
                actionLabel = context.getString(R.string.learn_more),
                long = true
            )
            // Let the user know they did it right since this process is confusing
            if (learnMore && effects.requestPermission(AppPermission.IgnoreBatteryOptimizations)) {
                effects.showMessage(context.getString(R.string.battery_usage_allowed))
            }
        }
    }
}
