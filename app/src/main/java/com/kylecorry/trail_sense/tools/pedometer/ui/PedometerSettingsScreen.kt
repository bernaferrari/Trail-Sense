@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.IPedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcastEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
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
fun PedometerSettingsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val viewModel = viewModel {
        PedometerSettingsViewModel(
            prefs = UserPreferences(appContext),
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            pedometer = PedometerSubsystem.getInstance(appContext),
            status = object : PedometerSystemStatus {
                override fun canRecognizeActivity() = Permissions.canRecognizeActivity(appContext)
                override fun isBackgroundLocationEnabled() = Permissions.isBackgroundLocationEnabled(appContext)
                override fun isNotificationChannelBlocked() =
                    Notify.isChannelBlocked(appContext, StepCounterService.CHANNEL_ID)
            },
            keys = PedometerSettingsKeys(
                resetDaily = appContext.getString(R.string.pref_odometer_reset_daily),
                useAlarm = appContext.getString(R.string.pref_pedometer_use_alarm_for_distance_alert)
            )
        )
    }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }
    val strideLengthUnits = remember(formatter) { formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    // The pedometer can be turned on or off elsewhere (the quick action); the switch follows while resumed.
    ToolBroadcastEffect(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED) { viewModel.onEnabledChanged(true) }
    ToolBroadcastEffect(PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED) { viewModel.onEnabledChanged(false) }

    fun averagePaceTimeLabel(mode: AveragePaceTimeMode): String {
        return context.getString(
            when (mode) {
                AveragePaceTimeMode.Active -> R.string.active_time
                AveragePaceTimeMode.Elapsed -> R.string.elapsed_time
            }
        )
    }

    val pedometerTitle = stringResource(R.string.pedometer)
    val distanceTitle = stringResource(R.string.distance)
    val paceTitle = stringResource(R.string.pref_pedometer_average_pace_time_title)
    val paceChoices = AveragePaceTimeMode.entries.map {
        TrailSenseChoice(it, averagePaceTimeLabel(it))
    }

    TrailSensePage(
        title = pedometerTitle,
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
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
                                            context,
                                            context.getString(R.string.pedometer),
                                            context.getString(R.string.pedometer_disclaimer),
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
            if (viewModel.permissionVisible) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.request_permission),
                        onClick = { context.startActivity(Intents.appSettings(context)) },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.notifications_channel, pedometerTitle),
                    selected = stringResource(if (viewModel.notificationsBlocked) R.string.off else R.string.on),
                    onClick = {
                        context.startActivity(
                            Intents.notificationSettings(context, StepCounterService.CHANNEL_ID)
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
                    selected = formatter.formatDistance(viewModel.strideLength, 2),
                    onClick = {
                        CustomUiUtils.pickDistance(
                            context,
                            strideLengthUnits,
                            viewModel.strideLength,
                            context.getString(R.string.pref_stride_length_title),
                            showFeetAndInches = true
                        ) { distance, cancelled ->
                            if (cancelled || distance == null || distance.value <= 0f) {
                                return@pickDistance
                            }
                            viewModel.changeStrideLength(distance)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.estimate_stride_length),
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
                    label = stringResource(R.string.pref_odometer_reset_daily_title),
                    checked = viewModel.resetDaily,
                    onCheckedChange = viewModel::changeResetDaily,
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_pedometer_history_days_title),
                    selected = formatter.formatDays(viewModel.stepHistoryDays),
                    onClick = {
                        Pickers.number(
                            context,
                            context.getString(R.string.pref_pedometer_history_days_title),
                            null,
                            viewModel.stepHistoryDays,
                            allowDecimals = false,
                            allowNegative = false,
                            hint = context.getString(R.string.days)
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
                    label = stringResource(R.string.use_alarm_for_distance_alert),
                    summary = stringResource(R.string.alarm_description),
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
internal interface PedometerSystemStatus {
    fun canRecognizeActivity(): Boolean
    fun isBackgroundLocationEnabled(): Boolean
    fun isNotificationChannelBlocked(): Boolean
}

/** The preference keys the page writes directly (resolved from resources by the screen). */
internal data class PedometerSettingsKeys(
    val resetDaily: String,
    val useAlarm: String
)

/**
 * The pedometer settings: the switch (and whether a permission is still missing), stride length, daily reset,
 * history length, the distance alert alarm and the average pace time. The permission row is rechecked often while
 * the page is resumed, since the permission is granted in system settings.
 */
internal class PedometerSettingsViewModel(
    private val prefs: UserPreferences,
    private val cache: IPreferences,
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
    var stepHistoryDays by mutableStateOf(prefs.pedometer.stepHistory.toDays().toInt())
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
        prefs.pedometer.stepHistory = Duration.ofDays(dayCount.toLong())
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
