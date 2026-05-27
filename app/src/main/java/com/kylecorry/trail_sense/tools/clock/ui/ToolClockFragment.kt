package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.background.AlarmTaskScheduler
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useBooleanPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useEffectWithCleanup
import com.kylecorry.trail_sense.shared.extensions.compose.useIntPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useLifecycleEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.permissions.requestScheduleExactAlarms
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.tools.clock.ui.ClockView
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ToolClockFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val prefs = useService<UserPreferences>()
        val sensorService = useService<SensorService>()
        val gps = remember { sensorService.getGPS() }
        val formatService = remember { FormatService.getInstance(requireContext()) }

        var gpsTime by remember { mutableStateOf(Instant.now()) }
        var systemTime by remember { mutableStateOf(Instant.now()) }
        var isGpsTime by remember { mutableStateOf(false) }
        var isUpdating by remember { mutableStateOf(true) }

        val showAnalog by remember { mutableStateOf(prefs.clock.showAnalogClock) }
        val use24Hour by remember { mutableStateOf(prefs.use24HourTime) }

        val (brightness, _) = useIntPreference(stringResource(R.string.pref_screen_torch_brightness))
        val (isRed, _) = useBooleanPreference("cache_red_light")

        useEffect(gps) {
            gps.start {
                gpsTime = getGPSTime()
                systemTime = Instant.now()
                isGpsTime = true

                if (gps is CustomGPS && (gps as CustomGPS).isTimedOut) {
                    CustomUiUtils.oneTimeToast(requireContext(), getString(R.string.no_gps_signal), "clock_gps_timeout")
                    gpsTime = Instant.now()
                    isGpsTime = false
                }

                isUpdating = false
                true
            }
            isUpdating = true
        }

        useLifecycleEffect(androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
            gps.stop { true }
        }

        var liveTime by remember { mutableStateOf(Instant.now()) }

        useEffectWithCleanup {
            val timer = com.kylecorry.andromeda.core.time.CoroutineTimer {
                liveTime = Instant.now()
            }
            timer.interval(1000)
            return@useEffectWithCleanup { timer.stop() }
        }

        val currentTime = remember(gpsTime, systemTime, liveTime) {
            val diff = Duration.between(systemTime, liveTime)
            gpsTime.plus(diff)
        }

        ToolClockContent(
            currentTime = currentTime,
            isGpsTime = isGpsTime,
            isUpdating = isUpdating,
            showAnalogClock = showAnalog,
            use24Hour = use24Hour,
            formatService = formatService,
            onSyncGPS = {
                gps.start { true }
                isUpdating = true
            },
            onRequestPIP = {
                requestScheduleExactAlarms {
                    sendNextMinuteNotification(gpsTime, systemTime, formatService)
                }
            }
        )
    }

    private fun getGPSTime(): Instant {
        val gps = SensorService(requireContext()).getGPS()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tryOrNothing {
                return SystemClock.currentGnssTimeClock().instant()
            }
        }
        val fixTime = gps.fixTimeElapsedNanos ?: return gps.time
        val systemTime = SystemClock.elapsedRealtimeNanos()
        val timeDiff = fixTime - systemTime
        return gps.time.plusNanos(timeDiff)
    }

    private fun sendNextMinuteNotification(
        gpsTime: Instant,
        systemTime: Instant,
        formatService: FormatService
    ) {
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val clockError = Duration.between(systemTime, gpsTime)
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        val displayTimeWithSeconds = myTime.toLocalDateTime()
        var displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        if (displayTimeWithSeconds.second > 45) {
            displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(2)
        }
        val sendTime = displayTime.minus(clockError)
        val formattedTime = formatService.formatTime(displayTime.toLocalTime())

        Alerts.dialog(
            requireContext(),
            getString(R.string.clock_sync_time_settings),
            getString(R.string.clock_sync_instructions, formattedTime)
        ) { cancelled ->
            if (!cancelled) {
                Alerts.toast(requireContext(), getString(R.string.pip_notification_scheduled, formattedTime))
                val scheduler = AlarmTaskScheduler(requireContext()) {
                    NextMinuteBroadcastReceiver.pendingIntent(requireContext(), formattedTime)
                }
                scheduler.once(sendTime.toZonedDateTime().toInstant())
                getResult(Intent(Settings.ACTION_DATE_SETTINGS)) { _, _ -> }
            }
        }
    }
}

@Composable
private fun ToolClockContent(
    currentTime: Instant,
    isGpsTime: Boolean,
    isUpdating: Boolean,
    showAnalogClock: Boolean,
    use24Hour: Boolean,
    formatService: FormatService,
    onSyncGPS: () -> Unit,
    onRequestPIP: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoned = remember(currentTime) {
        ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatService.formatTime(zoned.toLocalTime()),
                style = MaterialTheme.typography.headlineMedium
            )
            TrailSenseButton(
                text = stringResource(R.string.reload),
                onClick = onSyncGPS,
                modifier = Modifier.size(height = 36.dp, width = 100.dp)
            )
        }

        Spacer(Modifier.height(TrailSenseSpacing.xl))

        if (showAnalogClock) {
            AndroidView(
                factory = { ctx ->
                    ClockView(ctx).apply {
                        time = zoned.toLocalTime()
                        use24Hours = use24Hour
                    }
                },
                update = { view ->
                    view.time = zoned.toLocalTime()
                    view.use24Hours = use24Hour
                },
                modifier = Modifier.size(240.dp)
            )
        } else {
            Text(
                text = formatService.formatTime(zoned.toLocalTime()),
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(Modifier.height(TrailSenseSpacing.md))

        Text(
            text = if (isGpsTime) stringResource(R.string.clock_source_gps) else stringResource(R.string.clock_source_device),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isUpdating) {
            Spacer(Modifier.height(TrailSenseSpacing.sm))
            Text(
                text = stringResource(R.string.clock_waiting_for_gps),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(TrailSenseSpacing.xl))

        TrailSenseButton(
            text = stringResource(R.string.pip_button),
            onClick = onRequestPIP
        )
    }
}

internal data class ClockOffset(val seconds: Long, val direction: ClockOffsetDirection)

internal enum class ClockOffsetDirection {
    Accurate,
    Slow,
    Fast
}

private fun getClockOffset(duration: Duration): ClockOffset {
    val seconds = duration.abs().seconds
    val direction = when {
        seconds == 0L -> ClockOffsetDirection.Accurate
        duration.isNegative -> ClockOffsetDirection.Fast
        else -> ClockOffsetDirection.Slow
    }
    return ClockOffset(seconds, direction)
}
