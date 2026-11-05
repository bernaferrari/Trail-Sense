@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.trail_sense.shared.navigation.Route

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.goBack
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import java.time.Duration
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun PedometerSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PedometerSettingsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    PedometerSettingsContent(controller, host)
}

private class PedometerSettingsController(private val host: TrailSenseDestinationHost) {

    val userPrefs by lazy { UserPreferences(host.requireContext()) }
    val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val strideLengthUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
    }

    var setEnabledState: ((Boolean) -> Unit)? = null
    var refreshPermissionState: (() -> Unit)? = null
    var refreshNotificationState: (() -> Unit)? = null

    private val intervalometer = CoroutineTimer {
        refreshPermissionState?.invoke()
    }

    fun onResume() {
        refreshPermissionState?.invoke()
        refreshNotificationState?.invoke()
        intervalometer.interval(20)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED, ::onPedometerEnabled)
        Tools.subscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    fun onPause() {
        intervalometer.stop()
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED,
            ::onPedometerEnabled
        )
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    fun onDestroy() {
        onPause()
        setEnabledState = null
        refreshPermissionState = null
        refreshNotificationState = null
    }

    fun onPedometerEnabledChanged(
        enabled: Boolean,
        updateEnabled: (Boolean) -> Unit
    ) {
        if (enabled) {
            host.requestActivityRecognition { hasPermission ->
                if (hasPermission) {
                    if (cache.getBoolean("pedometer_battery_sent") != true) {
                        Alerts.dialog(
                            host.requireContext(),
                            host.getString(R.string.pedometer),
                            host.getString(R.string.pedometer_disclaimer),
                            cancelText = null
                        )
                        cache.putBoolean("pedometer_battery_sent", true)
                    }
                    PedometerSubsystem.getInstance(host.requireContext()).enable()
                } else {
                    PedometerSubsystem.getInstance(host.requireContext()).disable()
                    updateEnabled(false)
                    host.alertNoActivityRecognitionPermission()
                }
            }
        } else {
            PedometerSubsystem.getInstance(host.requireContext()).disable()
        }
    }

    fun pickStrideLength(
        current: Distance,
        onPicked: (Distance) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            host.requireContext(),
            strideLengthUnits,
            current,
            host.getString(R.string.pref_stride_length_title),
            showFeetAndInches = true
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0f) {
                return@pickDistance
            }

            onPicked(distance)
        }
    }

    fun formatStrideLength(distance: Distance): String {
        return formatter.formatDistance(distance, 2)
    }

    fun averagePaceTimeLabel(mode: AveragePaceTimeMode): String {
        return host.getString(
            when (mode) {
                AveragePaceTimeMode.Active -> R.string.active_time
                AveragePaceTimeMode.Elapsed -> R.string.elapsed_time
            }
        )
    }

    fun pickAveragePaceTime(onPicked: (AveragePaceTimeMode) -> Unit) {
        val modes = AveragePaceTimeMode.entries
        Pickers.item(
            host.requireContext(),
            host.getString(R.string.pref_pedometer_average_pace_time_title),
            modes.map { averagePaceTimeLabel(it) },
            modes.indexOf(userPrefs.pedometer.averagePaceTimeMode).coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                val mode = modes.getOrElse(index) { AveragePaceTimeMode.Active }
                userPrefs.pedometer.averagePaceTimeMode = mode
                onPicked(mode)
            }
        }
    }

    fun pickStepHistory(onPicked: (Int) -> Unit) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.pref_pedometer_history_days_title),
            null,
            userPrefs.pedometer.stepHistory.toDays().toInt(),
            allowDecimals = false,
            allowNegative = false,
            hint = host.getString(R.string.days)
        ) { days ->
            if (days != null) {
                val dayCount = if (days.toInt() > 0) days.toInt() else 1
                userPrefs.pedometer.stepHistory = Duration.ofDays(dayCount.toLong())
                onPicked(dayCount)
            }
        }
    }

    fun isPermissionRequestVisible(enabled: Boolean): Boolean {
        return (enabled && !Permissions.canRecognizeActivity(host.requireContext())) ||
                (!enabled && !Permissions.isBackgroundLocationEnabled(host.requireContext()))
    }

    fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(host.requireContext(), StepCounterService.CHANNEL_ID)
        return if (isBlocked) host.getString(R.string.off) else host.getString(R.string.on)
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(host.getString(key)) ?: defaultValue
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(host.getString(key), value)
    }

    private fun onPedometerEnabled(_data: Bundle): Boolean {
        setEnabledState?.invoke(true)
        return true
    }

    private fun onPedometerDisabled(_data: Bundle): Boolean {
        setEnabledState?.invoke(false)
        return true
    }
}

@Composable
private fun PedometerSettingsContent(
    controller: PedometerSettingsController,
    host: TrailSenseDestinationHost
) {
    val userPrefs = controller.userPrefs
    var enabled by remember { mutableStateOf(userPrefs.pedometer.isEnabled) }
    var strideLength by remember {
        mutableStateOf(userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits))
    }
    var resetDaily by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_odometer_reset_daily))
    }
    var useAlarm by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_pedometer_use_alarm_for_distance_alert))
    }
    var averagePaceTimeMode by remember { mutableStateOf(userPrefs.pedometer.averagePaceTimeMode) }
    var stepHistoryDays by remember { mutableStateOf(userPrefs.pedometer.stepHistory.toDays().toInt()) }
    var permissionVisible by remember { mutableStateOf(controller.isPermissionRequestVisible(enabled)) }
    var notificationSummary by remember { mutableStateOf(controller.notificationSummary()) }

    controller.setEnabledState = {
        enabled = it
        permissionVisible = controller.isPermissionRequestVisible(it)
    }
    controller.refreshPermissionState = {
        permissionVisible = controller.isPermissionRequestVisible(enabled)
    }
    controller.refreshNotificationState = {
        notificationSummary = controller.notificationSummary()
    }

    val pedometerTitle = stringResource(R.string.pedometer)
    val distanceTitle = stringResource(R.string.distance)
    val paceTitle = stringResource(R.string.pref_pedometer_average_pace_time_title)
    val paceChoices = AveragePaceTimeMode.entries.map {
        TrailSenseChoice(it, controller.averagePaceTimeLabel(it))
    }

    TrailSensePage(
        title = pedometerTitle,
        onBack = host::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("pedometer") {
            row { position ->
                SettingsSwitchRow(
                    label = pedometerTitle,
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        userPrefs.pedometer.isEnabled = it
                        controller.onPedometerEnabledChanged(it) { newEnabled ->
                            enabled = newEnabled
                            permissionVisible = controller.isPermissionRequestVisible(newEnabled)
                        }
                        permissionVisible = controller.isPermissionRequestVisible(it)
                    },
                    position = position
                )
            }
            if (permissionVisible) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.request_permission),
                        onClick = { host.startActivity(Intents.appSettings(host.requireContext())) },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.notifications_channel, pedometerTitle),
                    selected = notificationSummary,
                    onClick = {
                        host.startActivity(
                            Intents.notificationSettings(host.requireContext(), StepCounterService.CHANNEL_ID)
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("distance", title = distanceTitle) {
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_stride_length_title),
                    selected = controller.formatStrideLength(strideLength),
                    onClick = {
                        controller.pickStrideLength(strideLength) {
                            strideLength = it
                            userPrefs.pedometer.strideLength = it
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.estimate_stride_length),
                    onClick = {
                        host.findNavigator().navigate(
                            Route.StrideLengthEstimation
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_odometer_reset_daily_title),
                    checked = resetDaily,
                    onCheckedChange = {
                        resetDaily = it
                        controller.putBoolean(R.string.pref_odometer_reset_daily, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_pedometer_history_days_title),
                    selected = controller.formatter.formatDays(stepHistoryDays),
                    onClick = { controller.pickStepHistory { stepHistoryDays = it } },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.use_alarm_for_distance_alert),
                    summary = stringResource(R.string.alarm_description),
                    checked = useAlarm,
                    onCheckedChange = {
                        useAlarm = it
                        controller.putBoolean(R.string.pref_pedometer_use_alarm_for_distance_alert, it)
                    },
                    position = position
                )
            }
        }

        trailSenseSection("pace", title = paceTitle) {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    TrailSenseConnectedChoice(
                        options = paceChoices,
                        selected = averagePaceTimeMode,
                        label = paceTitle,
                        onSelect = {
                            userPrefs.pedometer.averagePaceTimeMode = it
                            averagePaceTimeMode = it
                        }
                    )
                }
            }
        }
    }
}
