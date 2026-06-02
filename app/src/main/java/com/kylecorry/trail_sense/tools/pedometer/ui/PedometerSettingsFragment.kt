package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
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
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class PedometerSettingsFragment : Fragment() {

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val strideLengthUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
    }

    private var setEnabledState: ((Boolean) -> Unit)? = null
    private var refreshPermissionState: (() -> Unit)? = null
    private var refreshNotificationState: (() -> Unit)? = null

    private val intervalometer = CoroutineTimer {
        refreshPermissionState?.invoke()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var enabled by remember { mutableStateOf(userPrefs.pedometer.isEnabled) }
                    var strideLength by remember {
                        mutableStateOf(
                            userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits)
                        )
                    }
                    var resetDaily by remember {
                        mutableStateOf(getBoolean(R.string.pref_odometer_reset_daily))
                    }
                    var useAlarm by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_pedometer_use_alarm_for_distance_alert)
                        )
                    }
                    var permissionVisible by remember {
                        mutableStateOf(isPermissionRequestVisible(enabled))
                    }
                    var notificationSummary by remember {
                        mutableStateOf(notificationSummary())
                    }

                    setEnabledState = {
                        enabled = it
                        permissionVisible = isPermissionRequestVisible(it)
                    }
                    refreshPermissionState = {
                        permissionVisible = isPermissionRequestVisible(enabled)
                    }
                    refreshNotificationState = {
                        notificationSummary = notificationSummary()
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
                                        onPedometerEnabledChanged(it) { newEnabled ->
                                            enabled = newEnabled
                                            permissionVisible =
                                                isPermissionRequestVisible(newEnabled)
                                        }
                                        permissionVisible = isPermissionRequestVisible(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("stride_length") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_stride_length_title),
                                    summary = formatStrideLength(strideLength),
                                    onClick = {
                                        pickStrideLength(strideLength) {
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
                                        findNavController().navigate(
                                            R.id.action_calibrate_pedometer_to_estimate_stride_length
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("notifications") {
                                SettingsActionRow(
                                    label = getString(
                                        R.string.notifications_channel,
                                        getString(R.string.pedometer)
                                    ),
                                    summary = notificationSummary,
                                    onClick = {
                                        startActivity(
                                            Intents.notificationSettings(
                                                requireContext(),
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
                                            startActivity(Intents.appSettings(requireContext()))
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
                                        putBoolean(R.string.pref_odometer_reset_daily, it)
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
                                        putBoolean(
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
            }
        }
    }

    private fun setupAveragePaceTimeSetting() {
        val names = mapOf(
            AveragePaceTimeMode.Active to getString(R.string.active_time),
            AveragePaceTimeMode.Elapsed to getString(R.string.elapsed_time)
        )
        val averagePaceTime = list(R.string.pref_pedometer_average_pace_time)
        averagePaceTime?.entries = names.values.toTypedArray()
        averagePaceTime?.entryValues = names.keys.map { it.id.toString() }.toTypedArray()
    }

    private fun setupStepHistorySetting() {
        val stepHistory = preference(R.string.pref_pedometer_history_days)
        stepHistory?.summary =
            formatService.formatDays(userPrefs.pedometer.stepHistory.toDays().toInt())
        stepHistory?.setOnPreferenceClickListener {
            Pickers.number(
                requireContext(),
                it.title.toString(),
                null,
                userPrefs.pedometer.stepHistory.toDays().toInt(),
                allowDecimals = false,
                allowNegative = false,
                hint = getString(R.string.days)
            ) { days ->
                if (days != null) {
                    userPrefs.pedometer.stepHistory = Duration.ofDays(days.toLong())
                    it.summary = formatService.formatDays(if (days.toInt() > 0) days.toInt() else 1)
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState?.invoke()
        refreshNotificationState?.invoke()
        intervalometer.interval(20)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED, ::onPedometerEnabled)
        Tools.subscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED,
            ::onPedometerEnabled
        )
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    override fun onDestroyView() {
        setEnabledState = null
        refreshPermissionState = null
        refreshNotificationState = null
        super.onDestroyView()
    }

    private fun onPedometerEnabledChanged(
        enabled: Boolean,
        updateEnabled: (Boolean) -> Unit
    ) {
        if (enabled) {
            requestActivityRecognition { hasPermission ->
                if (hasPermission) {
                    if (cache.getBoolean("pedometer_battery_sent") != true) {
                        Alerts.dialog(
                            requireContext(),
                            getString(R.string.pedometer),
                            getString(R.string.pedometer_disclaimer),
                            cancelText = null
                        )
                        cache.putBoolean("pedometer_battery_sent", true)
                    }
                    PedometerSubsystem.getInstance(requireContext()).enable()
                } else {
                    PedometerSubsystem.getInstance(requireContext()).disable()
                    updateEnabled(false)
                    alertNoActivityRecognitionPermission()
                }
            }
        } else {
            PedometerSubsystem.getInstance(requireContext()).disable()
        }
    }

    private fun pickStrideLength(
        current: Distance,
        onPicked: (Distance) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            requireContext(),
            strideLengthUnits,
            current,
            getString(R.string.pref_stride_length_title),
            showFeetAndInches = true
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0f) {
                return@pickDistance
            }

            onPicked(distance)
        }
    }

    private fun formatStrideLength(distance: Distance): String {
        return formatter.formatDistance(distance, 2)
    }

    private fun isPermissionRequestVisible(enabled: Boolean): Boolean {
        return (enabled && !Permissions.canRecognizeActivity(requireContext())) ||
                (!enabled && !Permissions.isBackgroundLocationEnabled(requireContext()))
    }

    private fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(requireContext(), StepCounterService.CHANNEL_ID)
        return if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    private fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(getString(key)) ?: defaultValue
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
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
