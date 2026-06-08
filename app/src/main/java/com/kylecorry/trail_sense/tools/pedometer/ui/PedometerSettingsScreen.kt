@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

@Composable
fun PedometerSettingsToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { PedometerSettingsController(fragment) }
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

    PedometerSettingsContent(controller, fragment)
}

private class PedometerSettingsController(private val fragment: TrailSenseComposeDestinationFragment) {

    val userPrefs by lazy { UserPreferences(fragment.requireContext()) }
    val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }
    val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
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
            fragment.requestActivityRecognition { hasPermission ->
                if (hasPermission) {
                    if (cache.getBoolean("pedometer_battery_sent") != true) {
                        Alerts.dialog(
                            fragment.requireContext(),
                            fragment.getString(R.string.pedometer),
                            fragment.getString(R.string.pedometer_disclaimer),
                            cancelText = null
                        )
                        cache.putBoolean("pedometer_battery_sent", true)
                    }
                    PedometerSubsystem.getInstance(fragment.requireContext()).enable()
                } else {
                    PedometerSubsystem.getInstance(fragment.requireContext()).disable()
                    updateEnabled(false)
                    fragment.alertNoActivityRecognitionPermission()
                }
            }
        } else {
            PedometerSubsystem.getInstance(fragment.requireContext()).disable()
        }
    }

    fun pickStrideLength(
        current: Distance,
        onPicked: (Distance) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            fragment.requireContext(),
            strideLengthUnits,
            current,
            fragment.getString(R.string.pref_stride_length_title),
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

    fun isPermissionRequestVisible(enabled: Boolean): Boolean {
        return (enabled && !Permissions.canRecognizeActivity(fragment.requireContext())) ||
                (!enabled && !Permissions.isBackgroundLocationEnabled(fragment.requireContext()))
    }

    fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(fragment.requireContext(), StepCounterService.CHANNEL_ID)
        return if (isBlocked) fragment.getString(R.string.off) else fragment.getString(R.string.on)
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(fragment.getString(key)) ?: defaultValue
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(fragment.getString(key), value)
    }

    private fun onPedometerEnabled(data: Bundle): Boolean {
        setEnabledState?.invoke(true)
        return true
    }

    private fun onPedometerDisabled(data: Bundle): Boolean {
        setEnabledState?.invoke(false)
        return true
    }
}

@Composable
private fun PedometerSettingsContent(
    controller: PedometerSettingsController,
    fragment: TrailSenseComposeDestinationFragment
) {
                    val userPrefs = controller.userPrefs
                    var enabled by remember { mutableStateOf(userPrefs.pedometer.isEnabled) }
                    var strideLength by remember {
                        mutableStateOf(
                            userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits)
                        )
                    }
                    var resetDaily by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_odometer_reset_daily))
                    }
                    var useAlarm by remember {
                        mutableStateOf(
                            controller.getBoolean(R.string.pref_pedometer_use_alarm_for_distance_alert)
                        )
                    }
                    var permissionVisible by remember {
                        mutableStateOf(controller.isPermissionRequestVisible(enabled))
                    }
                    var notificationSummary by remember {
                        mutableStateOf(controller.notificationSummary())
                    }

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
                                TrailSenseSectionHeader(title = stringResource(R.string.pedometer))
                            }
                            item("enabled") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pedometer),
                                    checked = enabled,
                                    onCheckedChange = {
                                        enabled = it
                                        userPrefs.pedometer.isEnabled = it
                                        controller.onPedometerEnabledChanged(it) { newEnabled ->
                                            enabled = newEnabled
                                            permissionVisible =
                                                controller.isPermissionRequestVisible(newEnabled)
                                        }
                                        permissionVisible = controller.isPermissionRequestVisible(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("stride_length") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_stride_length_title),
                                    summary = controller.formatStrideLength(strideLength),
                                    onClick = {
                                        controller.pickStrideLength(strideLength) {
                                            strideLength = it
                                            userPrefs.pedometer.strideLength = it
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("estimate_stride") {
                                SettingsActionRow(
                                    label = stringResource(R.string.estimate_stride_length),
                                    onClick = {
                                        fragment.findNavController().navigate(
                                            R.id.action_calibrate_pedometer_to_estimate_stride_length
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("notifications") {
                                SettingsActionRow(
                                    label = fragment.getString(
                                        R.string.notifications_channel,
                                        fragment.getString(R.string.pedometer)
                                    ),
                                    summary = notificationSummary,
                                    onClick = {
                                        fragment.startActivity(
                                            Intents.notificationSettings(
                                                fragment.requireContext(),
                                                StepCounterService.CHANNEL_ID
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (permissionVisible) {
                                item("permission") {
                                    SettingsActionRow(
                                        label = stringResource(R.string.request_permission),
                                        onClick = {
                                            fragment.startActivity(Intents.appSettings(fragment.requireContext()))
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                            }
                            item("reset_daily") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_odometer_reset_daily_title),
                                    checked = resetDaily,
                                    onCheckedChange = {
                                        resetDaily = it
                                        controller.putBoolean(R.string.pref_odometer_reset_daily, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("alarm") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.use_alarm_for_distance_alert),
                                    summary = stringResource(R.string.alarm_description),
                                    checked = useAlarm,
                                    onCheckedChange = {
                                        useAlarm = it
                                        controller.putBoolean(
                                            R.string.pref_pedometer_use_alarm_for_distance_alert,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
}
