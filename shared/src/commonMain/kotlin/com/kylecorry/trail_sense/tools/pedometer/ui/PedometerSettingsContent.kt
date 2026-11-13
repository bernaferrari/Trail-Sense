@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.days

import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.runtime.rememberCoroutineScope
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import kotlinx.coroutines.launch

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.IPedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcastEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.pedometer.PedometerBroadcasts
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode

/**
 * Pedometer settings. [status] reports the platform's permissions and notification channel; [openAppSettings] and
 * [openNotificationSettings] open the system pages for them, and their rows are hidden when null.
 */
@Composable
fun PedometerSettingsScreen(
    status: PedometerSystemStatus,
    openAppSettings: (() -> Unit)?,
    openNotificationSettings: (() -> Unit)?
) {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val dispatcher = rememberSystemBack()
    val viewModel = viewModel {
        PedometerSettingsViewModel(
            prefs = getAppService<UserPreferences>(),
            cache = getAppService<Preferences>(),
            pedometer = getAppService<IPedometerSubsystem>(),
            status = status,
            keys = PedometerSettingsKeys(
                resetDaily = PreferenceKeys.odometerResetDaily,
                useAlarm = PreferenceKeys.pedometerUseAlarmForDistanceAlert
            )
        )
    }
    val formatter = remember { FormatService.getInstance() }
    val strideLengthUnits = remember(formatter) { formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    // The pedometer can be turned on or off elsewhere (the quick action); the switch follows while resumed.
    ToolBroadcastEffect(PedometerBroadcasts.PEDOMETER_ENABLED) { viewModel.onEnabledChanged(true) }
    ToolBroadcastEffect(PedometerBroadcasts.PEDOMETER_DISABLED) { viewModel.onEnabledChanged(false) }

    fun averagePaceTimeLabel(mode: AveragePaceTimeMode): String {
        return strings.get(
            when (mode) {
                AveragePaceTimeMode.Active -> Res.string.active_time
                AveragePaceTimeMode.Elapsed -> Res.string.elapsed_time
            }
        )
    }

    val pedometerTitle = appString(Res.string.pedometer)
    val distanceTitle = appString(Res.string.distance)
    val paceTitle = appString(Res.string.pref_pedometer_average_pace_time_title)
    val paceChoices = AveragePaceTimeMode.entries.map {
        TrailSenseChoice(it, averagePaceTimeLabel(it))
    }

    TrailSensePage(
        title = pedometerTitle,
        onBack = { dispatcher?.invoke() },
        backLabel = appString(Res.string.back)
    ) {
        trailSenseSection("pedometer") {
            row { position ->
                SettingsSwitchRow(
                    label = pedometerTitle,
                    checked = viewModel.enabled,
                    onCheckedChange = {
                        viewModel.changeEnabled(it)
                        if (it) {
                            scope.launch {
                                if (effects.requestPermission(AppPermission.ActivityRecognition)) {
                                    if (viewModel.shouldShowDisclaimer()) {
                                        Alerts.dialog(
                                            host,
                                            getAppService<AppStrings>().get(Res.string.pedometer),
                                            getAppService<AppStrings>().get(Res.string.pedometer_disclaimer),
                                            cancelText = null
                                        )
                                        viewModel.onDisclaimerShown()
                                    }
                                    viewModel.startPedometer()
                                } else {
                                    viewModel.onPermissionDenied()
                                    effects.explainPermissionDenied(AppPermission.ActivityRecognition)
                                }
                            }
                        }
                    },
                    position = position
                )
            }
            if (viewModel.permissionVisible && openAppSettings != null) {
                row { position ->
                    SettingsActionRow(
                        label = appString(Res.string.request_permission),
                        onClick = openAppSettings,
                        position = position
                    )
                }
            }
            if (openNotificationSettings != null) {
                row { position ->
                    SettingsPickerRow(
                        label = appString(Res.string.notifications_channel, pedometerTitle),
                        selected = appString(if (viewModel.notificationsBlocked) Res.string.off else Res.string.on),
                        onClick = openNotificationSettings,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("distance", title = distanceTitle) {
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.pref_step_length_title),
                    selected = formatter.formatDistance(viewModel.strideLength, 2),
                    onClick = {
                        UnitPickers.distance(
                            host,
                            strideLengthUnits,
                            viewModel.strideLength,
                            getAppService<AppStrings>().get(Res.string.pref_step_length_title),
                            showFeetAndInches = true
                        ) { distance, cancelled ->
                            if (cancelled || distance == null || distance.value <= 0f) {
                                return@distance
                            }
                            viewModel.changeStrideLength(distance)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = appString(Res.string.estimate_step_length),
                    onClick = {
                        navigator.navigate(
                            Route.StrideLengthEstimation
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_odometer_reset_daily_title),
                    checked = viewModel.resetDaily,
                    onCheckedChange = viewModel::changeResetDaily,
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.pref_pedometer_history_days_title),
                    selected = formatter.formatDays(viewModel.stepHistoryDays),
                    onClick = {
                        Pickers.number(
                            host,
                            getAppService<AppStrings>().get(Res.string.pref_pedometer_history_days_title),
                            null,
                            viewModel.stepHistoryDays,
                            allowDecimals = false,
                            allowNegative = false,
                            hint = getAppService<AppStrings>().get(Res.string.days)
                        ) { days ->
                            if (days != null) {
                                viewModel.changeStepHistory(days.toInt())
                            }
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.use_alarm_for_distance_alert),
                    summary = appString(Res.string.alarm_description),
                    checked = viewModel.useAlarm,
                    onCheckedChange = viewModel::changeUseAlarm,
                    position = position
                )
            }
        }

        trailSenseSection("pace", title = paceTitle) {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    TrailSenseConnectedChoice(
                        options = paceChoices,
                        selected = viewModel.averagePaceTimeMode,
                        label = paceTitle,
                        onSelect = viewModel::changeAveragePaceTimeMode
                    )
                }
            }
        }
    }
}

/** The platform state the page shows: permissions and whether the step counter's notifications are blocked. */
interface PedometerSystemStatus {
    fun canRecognizeActivity(): Boolean
    fun isBackgroundLocationEnabled(): Boolean
    fun isNotificationChannelBlocked(): Boolean
}

/** The preference keys the page writes directly (resolved from resources by the screen). */
data class PedometerSettingsKeys(
    val resetDaily: String,
    val useAlarm: String
)

/**
 * The pedometer settings: the switch (and whether a permission is still missing), stride length, daily reset,
 * history length, the distance alert alarm and the average pace time. The permission row is rechecked often while
 * the page is resumed, since the permission is granted in system settings.
 */
class PedometerSettingsViewModel(
    private val prefs: UserPreferences,
    private val cache: Preferences,
    private val pedometer: IPedometerSubsystem,
    private val status: PedometerSystemStatus,
    private val keys: PedometerSettingsKeys
) : ViewModel() {

    var enabled by mutableStateOf(prefs.pedometer.isEnabled)
        private set
    var strideLength by mutableStateOf(prefs.pedometer.strideLength.convertTo(prefs.baseDistanceUnits))
        private set
    var resetDaily by mutableStateOf(cache.getBoolean(keys.resetDaily) ?: false)
        private set
    var useAlarm by mutableStateOf(cache.getBoolean(keys.useAlarm) ?: false)
        private set
    var averagePaceTimeMode by mutableStateOf(prefs.pedometer.averagePaceTimeMode)
        private set
    var stepHistoryDays by mutableStateOf(prefs.pedometer.stepHistory.inWholeDays.toInt())
        private set
    var permissionVisible by mutableStateOf(isPermissionRequestVisible(enabled))
        private set
    var notificationsBlocked by mutableStateOf(status.isNotificationChannelBlocked())
        private set

    private var pollJob: Job? = null

    fun onResume() {
        refreshPermission()
        notificationsBlocked = status.isNotificationChannelBlocked()
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                refreshPermission()
                delay(PERMISSION_POLL_INTERVAL)
            }
        }
    }

    fun onPause() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Saves the switch; turning it off stops the pedometer, turning it on is finished by [startPedometer]. */
    fun changeEnabled(value: Boolean) {
        enabled = value
        prefs.pedometer.isEnabled = value
        if (!value) {
            pedometer.disable()
        }
        refreshPermission()
    }

    /** The pedometer was turned on or off elsewhere. */
    fun onEnabledChanged(value: Boolean) {
        enabled = value
        refreshPermission()
    }

    fun shouldShowDisclaimer(): Boolean = cache.getBoolean(DISCLAIMER_SHOWN) != true

    fun onDisclaimerShown() {
        cache.putBoolean(DISCLAIMER_SHOWN, true)
    }

    fun startPedometer() {
        pedometer.enable()
    }

    fun onPermissionDenied() {
        pedometer.disable()
        enabled = false
        refreshPermission()
    }

    fun changeStrideLength(value: Distance) {
        strideLength = value
        prefs.pedometer.strideLength = value
    }

    fun changeResetDaily(value: Boolean) {
        resetDaily = value
        cache.putBoolean(keys.resetDaily, value)
    }

    fun changeUseAlarm(value: Boolean) {
        useAlarm = value
        cache.putBoolean(keys.useAlarm, value)
    }

    fun changeStepHistory(days: Int) {
        val dayCount = if (days > 0) days else 1
        prefs.pedometer.stepHistory = (dayCount.toLong()).days
        stepHistoryDays = dayCount
    }

    fun changeAveragePaceTimeMode(mode: AveragePaceTimeMode) {
        prefs.pedometer.averagePaceTimeMode = mode
        averagePaceTimeMode = mode
    }

    override fun onCleared() {
        onPause()
    }

    private fun refreshPermission() {
        permissionVisible = isPermissionRequestVisible(enabled)
    }

    private fun isPermissionRequestVisible(enabled: Boolean): Boolean {
        return (enabled && !status.canRecognizeActivity()) ||
            (!enabled && !status.isBackgroundLocationEnabled())
    }

    private companion object {
        const val DISCLAIMER_SHOWN = "pedometer_battery_sent"
        const val PERMISSION_POLL_INTERVAL = 20L
    }
}
