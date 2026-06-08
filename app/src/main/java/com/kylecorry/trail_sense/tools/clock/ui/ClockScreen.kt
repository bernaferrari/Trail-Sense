@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.background.AlarmTaskScheduler
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertExactAlarmsDenied
import com.kylecorry.trail_sense.shared.permissions.requirePermissionRequester
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ClockToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val context = LocalContext.current
    val formatService = remember { FormatService.getInstance(context) }
    val sensorService = remember { SensorService(context) }
    val gps = remember { sensorService.getGPS() }
    val prefs = remember { UserPreferences(context) }

    ClockScreen(
        formatService = formatService,
        prefs = prefs,
        gps = gps,
        getGpsTime = { getGPSTime(gps) },
        requestExactAlarms = { requestExactAlarms(fragment, it) },
        onScheduleNextMinute = { gpsTime, systemTime ->
            sendNextMinuteNotification(fragment, formatService, gpsTime, systemTime)
        }
    )
}

@Composable
private fun ClockScreen(
    formatService: FormatService,
    prefs: UserPreferences,
    gps: ISatelliteGPS,
    getGpsTime: () -> Instant,
    requestExactAlarms: ((Boolean) -> Unit) -> Unit,
    onScheduleNextMinute: (Instant, Instant) -> Unit
) {
    val context = LocalContext.current
    var gpsTime by remember { mutableStateOf(Instant.now()) }
    var systemTime by remember { mutableStateOf(Instant.now()) }
    var isGpsTime by remember { mutableStateOf(false) }
    var isUpdatingClock by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(Instant.now()) }

    fun onGpsUpdate(): Boolean {
        gpsTime = getGpsTime()
        systemTime = Instant.now()
        isGpsTime = true

        if (gps is CustomGPS && gps.isTimedOut) {
            Alerts.toast(context, context.getString(R.string.no_gps_signal))
            gpsTime = Instant.now()
            isGpsTime = false
        }

        isUpdatingClock = false
        return false
    }

    fun startGpsUpdate() {
        isUpdatingClock = true
        gps.start(::onGpsUpdate)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startGpsUpdate()
                Lifecycle.Event.ON_PAUSE -> gps.stop(::onGpsUpdate)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            gps.stop(::onGpsUpdate)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(20)
        }
    }

    val systemDiff = Duration.between(systemTime, now)
    val currentTime = gpsTime.plus(systemDiff)
    val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
    val timeText = formatService.formatTime(myTime.toLocalTime())
    val dateText = formatService.formatDate(myTime)
    val showAnalogClock = prefs.clock.showAnalogClock

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showAnalogClock) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { startGpsUpdate() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reload)
                        )
                    }
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAnalogClock) {
                    AnalogClock(
                        time = myTime.toLocalTime()
                    )
                } else {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        if (isGpsTime) R.string.clock_source_gps else R.string.clock_source_device
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isUpdatingClock) {
                Text(
                    text = stringResource(R.string.clock_waiting_for_gps),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = {
                        requestExactAlarms {
                            onScheduleNextMinute(gpsTime, systemTime)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(stringResource(R.string.pip_button))
                }
            }
        }
    }
}

@Composable
private fun AnalogClock(time: LocalTime) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.size(250.dp)) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(outline, radius = radius, center = center, style = Stroke(width = 4f))

        for (tick in 0 until 60) {
            val angle = tick / 60f * 360f - 90f
            val startRadius = radius * if (tick % 5 == 0) 0.86f else 0.92f
            val endRadius = radius * 0.96f
            drawLine(
                color = if (tick % 5 == 0) onSurface else outline,
                start = center.pointAt(startRadius, angle),
                end = center.pointAt(endRadius, angle),
                strokeWidth = if (tick % 5 == 0) 4f else 2f,
                cap = StrokeCap.Round
            )
        }

        val hour = time.hour % 12
        val hourAngle = ((hour + time.minute / 60f) / 12f) * 360f - 90f
        val minuteAngle = ((time.minute + time.second / 60f) / 60f) * 360f - 90f
        val secondAngle = (time.second / 60f) * 360f - 90f

        drawLine(
            color = onSurface,
            start = center,
            end = center.pointAt(radius * 0.48f, hourAngle),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary,
            start = center,
            end = center.pointAt(radius * 0.72f, minuteAngle),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondary,
            start = center,
            end = center.pointAt(radius * 0.8f, secondAngle),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(primary, radius = 7f, center = center)
    }
}

private fun Offset.pointAt(radius: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(
        x + (cos(radians) * radius).toFloat(),
        y + (sin(radians) * radius).toFloat()
    )
}

private fun getGPSTime(gps: ISatelliteGPS): Instant {
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
    fragment: TrailSenseComposeDestinationFragment,
    formatService: FormatService,
    gpsTime: Instant,
    systemTime: Instant
) {
    val context = fragment.requireContext()
    val systemDiff = Duration.between(systemTime, Instant.now())
    val currentTime = gpsTime.plus(systemDiff)
    val clockError = Duration.between(systemTime, gpsTime)
    val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
    val displayTimeWithSeconds = myTime.toLocalDateTime()
    var displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    val displayTimeSecond = displayTimeWithSeconds.second
    if (displayTimeSecond > 45) {
        displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(2)
    }
    val sendTime = displayTime.minus(clockError)
    val formattedTime = formatService.formatTime(displayTime.toLocalTime())

    Alerts.dialog(
        context,
        fragment.getString(R.string.clock_sync_time_settings),
        fragment.getString(R.string.clock_sync_instructions, formattedTime)
    ) { cancelled ->
        if (!cancelled) {
            Alerts.toast(
                context,
                fragment.getString(R.string.pip_notification_scheduled, formattedTime)
            )

            val scheduler = AlarmTaskScheduler(context) {
                NextMinuteBroadcastReceiver.pendingIntent(context, formattedTime)
            }
            scheduler.once(sendTime.toZonedDateTime().toInstant())
            fragment.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
    }
}

private fun requestExactAlarms(
    fragment: TrailSenseComposeDestinationFragment,
    action: (hasPermission: Boolean) -> Unit
) {
    fragment.requirePermissionRequester().requestPermission(
        SpecialPermission.SCHEDULE_EXACT_ALARMS,
        PermissionRationale(
            fragment.getString(
                R.string.allow_schedule_exact_alarms,
                fragment.getString(R.string.app_name)
            ),
            AppServiceRegistry.get<MarkdownService>().toMarkdown(
                fragment.getString(
                    R.string.allow_schedule_exact_alarms_instructions,
                    fragment.getString(R.string.app_name),
                    fragment.getString(R.string.settings)
                )
            ),
            ok = fragment.getString(R.string.settings),
        )
    ) {
        val isGranted = Permissions.hasPermission(
            fragment.requireContext(),
            SpecialPermission.SCHEDULE_EXACT_ALARMS
        )
        if (!isGranted) {
            fragment.alertExactAlarmsDenied()
        }
        action(isGranted)
    }
}
