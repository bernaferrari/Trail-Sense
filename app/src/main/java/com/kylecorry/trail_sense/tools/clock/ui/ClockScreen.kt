@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.clock.ui

import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.Alert
import com.kylecorry.trail_sense.shared.icons.trail.Satellite
import com.kylecorry.trail_sense.shared.icons.trail.ToolClock
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.background.AlarmTaskScheduler
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
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
fun ClockToolScreen(host: TrailSenseDestinationHost) {
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
        requestExactAlarms = { host.requestScheduleExactAlarms(it) },
        onScheduleNextMinute = { gpsTime, systemTime ->
            sendNextMinuteNotification(host, formatService, gpsTime, systemTime)
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

    // One question: is this clock right?
    TrailSensePage(
        title = stringResource(R.string.tool_clock_title),
        onBack = rememberTrailSenseBack(),
        actions = {
            // The instrumented test treats this as the toolbar's right button.
            IconButton(
                onClick = { startGpsUpdate() },
                modifier = Modifier.testTag("toolbar_right_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.reload)
                )
            }
        }
    ) {
        item("time") {
            // The drift between the device clock and GPS, shown on the face as a ghost second hand.
            val driftSeconds = if (isGpsTime && !isUpdatingClock) {
                Duration.between(gpsTime, systemTime).toMillis() / 1000f
            } else {
                0f
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.sm, bottom = TrailSenseSpacing.lg)
                    .testTag("clock_title"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                if (showAnalogClock) {
                    Surface(
                        shape = androidx.compose.material3.MaterialShapes.Cookie12Sided.toShape(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(bottom = TrailSenseSpacing.lg)
                            .size(320.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnalogClock(time = myTime.toLocalTime(), driftSeconds = driftSeconds)
                        }
                    }
                }
                Text(
                    text = timeText,
                    style = (if (showAnalogClock) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.displayLarge
                    }).copy(fontFeatureSettings = "tnum"),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                // The verdict: how far the device clock drifts from GPS, one quiet line under the time (the time is
                // the one display value), led by a check when it can be trusted and a warning when it can't.
                val offset = getClockOffset(Duration.between(systemTime, gpsTime))
                val trusted = isGpsTime && !isUpdatingClock && offset.seconds <= TRIVIAL_OFFSET_SECONDS
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = TrailSenseSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGpsTime && !isUpdatingClock) {
                        Icon(
                            imageVector = if (trusted) TrailIcons.Check else TrailIcons.Alert,
                            contentDescription = null,
                            tint = if (trusted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(end = TrailSenseSpacing.sm)
                                .size(20.dp)
                        )
                    }
                    Text(
                        text = when {
                            isUpdatingClock -> stringResource(R.string.clock_waiting_for_gps)
                            isGpsTime -> clockOffsetLabel(systemTime, gpsTime)
                            else -> stringResource(R.string.no_gps_signal)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item("source") {
            // The one action: set the system time from GPS.
            AnimatedVisibility(
                visible = !isUpdatingClock,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
            ) {
                // Emphasis follows importance: a trivial drift gets a tonal button, a real one the filled primary.
                val drift = getClockOffset(Duration.between(systemTime, gpsTime)).seconds
                val onClick = {
                    requestExactAlarms {
                        onScheduleNextMinute(gpsTime, systemTime)
                    }
                }
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("pip_button")
                if (isGpsTime && drift <= TRIVIAL_OFFSET_SECONDS) {
                    androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = buttonModifier) {
                        Text(stringResource(R.string.pip_button), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(onClick = onClick, modifier = buttonModifier) {
                        Text(stringResource(R.string.pip_button), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/**
 * The face: dots for the hours, a short thick primary hour hand, a long thin minute hand in the face's ink and a
 * thin tertiary second hand that ticks with a small spring overshoot. The lengths differ enough that the hands never
 * merge, even when they overlap. When the device clock drifts from GPS, a faint ghost second hand shows where the device
 * thinks it is.
 */
@Composable
private fun AnalogClock(time: LocalTime, driftSeconds: Float) {
    val face = MaterialTheme.colorScheme.onPrimaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val second = MaterialTheme.colorScheme.tertiary
    val container = MaterialTheme.colorScheme.primaryContainer

    // Unwrapped so 59 -> 0 keeps turning forward instead of spinning back.
    val tickTarget = remember { mutableStateOf(-1L) }
    val secondHand = remember { Animatable(0f) }
    val totalSeconds = time.toSecondOfDay().toLong()
    LaunchedEffect(totalSeconds) {
        val previous = tickTarget.value
        tickTarget.value = totalSeconds
        if (previous < 0L || kotlin.math.abs(totalSeconds - previous) > 2L) {
            secondHand.snapTo(time.second * 6f)
        } else {
            val step = ((totalSeconds - previous) * 6f)
            secondHand.animateTo(
                secondHand.targetValue + step,
                spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Canvas(modifier = Modifier.size(248.dp)) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Twelve hour marks as dots, the quarters larger: a face that reads at a glance without a ring of ticks.
        for (hourMark in 0 until 12) {
            val angle = hourMark / 12f * 360f - 90f
            val quarter = hourMark % 3 == 0
            drawCircle(
                color = face.copy(alpha = if (quarter) 0.9f else 0.5f),
                radius = if (quarter) 6.dp.toPx() else 3.5.dp.toPx(),
                center = center.pointAt(radius * 0.9f, angle)
            )
        }

        val hour = time.hour % 12
        val hourAngle = ((hour + time.minute / 60f) / 12f) * 360f - 90f
        val minuteAngle = ((time.minute + time.second / 60f) / 60f) * 360f - 90f
        val secondAngle = secondHand.value - 90f

        drawLine(
            color = face,
            start = center,
            end = center.pointAt(radius * 0.78f, minuteAngle),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary,
            start = center,
            end = center.pointAt(radius * 0.42f, hourAngle),
            strokeWidth = 12.dp.toPx(),
            cap = StrokeCap.Round
        )
        if (kotlin.math.abs(driftSeconds) >= 0.5f) {
            val ghostAngle = secondAngle + driftSeconds * 6f
            drawLine(
                color = second.copy(alpha = 0.32f),
                start = center,
                end = center.pointAt(radius * 0.8f, ghostAngle),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(second.copy(alpha = 0.32f), radius = 5.dp.toPx(), center = center.pointAt(radius * 0.8f, ghostAngle))
        }
        drawLine(
            color = second,
            start = center.pointAt(radius * 0.08f, secondAngle + 180f),
            end = center.pointAt(radius * 0.8f, secondAngle),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(second, radius = 6.dp.toPx(), center = center.pointAt(radius * 0.8f, secondAngle))
        // A clean pivot cap over every hand.
        drawCircle(face, radius = 10.dp.toPx(), center = center)
        drawCircle(container, radius = 4.dp.toPx(), center = center)
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

    if (gps.eventTimeElapsedNanos == 0L) {
        return gps.eventTime
    }
    val systemTime = SystemClock.elapsedRealtimeNanos()
    val timeDiff = gps.eventTimeElapsedNanos - systemTime
    return gps.eventTime.plusNanos(timeDiff)
}

private fun sendNextMinuteNotification(
    host: TrailSenseDestinationHost,
    formatService: FormatService,
    gpsTime: Instant,
    systemTime: Instant
) {
    val context = host.requireContext()
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
        host.getString(R.string.clock_sync_time_settings),
        host.getString(R.string.clock_sync_instructions, formattedTime)
    ) { cancelled ->
        if (!cancelled) {
            Alerts.toast(
                context,
                host.getString(R.string.pip_notification_scheduled, formattedTime)
            )

            val scheduler = AlarmTaskScheduler(context) {
                NextMinuteBroadcastReceiver.pendingIntent(context, formattedTime)
            }
            scheduler.once(sendTime.toZonedDateTime().toInstant())
            host.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
    }
}

@Composable
private fun clockOffsetLabel(systemTime: Instant, gpsTime: Instant): String {
    val offset = getClockOffset(Duration.between(systemTime, gpsTime))
    val seconds = offset.seconds.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    return when (offset.direction) {
        ClockOffsetDirection.Accurate -> stringResource(R.string.clock_device_time_accurate)
        ClockOffsetDirection.Slow -> pluralStringResource(
            R.plurals.clock_device_time_slow,
            seconds,
            seconds
        )
        ClockOffsetDirection.Fast -> pluralStringResource(
            R.plurals.clock_device_time_fast,
            seconds,
            seconds
        )
    }
}

/** A drift this small doesn't make the clock untrustworthy. */
private const val TRIVIAL_OFFSET_SECONDS = 2L

internal data class ClockOffset(val seconds: Long, val direction: ClockOffsetDirection)

internal enum class ClockOffsetDirection {
    Accurate,
    Slow,
    Fast
}

private fun getClockOffset(duration: Duration): ClockOffset {
    val seconds = kotlin.math.abs(duration.seconds)
    val direction = when {
        seconds == 0L -> ClockOffsetDirection.Accurate
        duration.isNegative -> ClockOffsetDirection.Fast
        else -> ClockOffsetDirection.Slow
    }
    return ClockOffset(seconds, direction)
}
