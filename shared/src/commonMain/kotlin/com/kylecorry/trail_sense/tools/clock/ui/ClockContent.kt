@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.clock.ui

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.appPluralString
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.time.toJavaSeconds
import com.kylecorry.trail_sense.shared.time.toInstant
import com.kylecorry.trail_sense.shared.time.minus
import com.kylecorry.trail_sense.shared.time.plus
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock

import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.Alert
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import com.kylecorry.trail_sense.shared.time.toLocalDateTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ClockScreen(
    formatService: FormatService,
    prefs: UserPreferences,
    gps: ISatelliteGPS,
    isGpsTimedOut: () -> Boolean,
    getGpsTime: () -> Instant,
    requestExactAlarms: ((Boolean) -> Unit) -> Unit,
    onScheduleNextMinute: ((Instant, Instant) -> Unit)?
) {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    var gpsTime by remember { mutableStateOf(Clock.System.now()) }
    var systemTime by remember { mutableStateOf(Clock.System.now()) }
    var isGpsTime by remember { mutableStateOf(false) }
    var isUpdatingClock by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(Clock.System.now()) }

    fun onGpsUpdate(): Boolean {
        gpsTime = getGpsTime()
        systemTime = Clock.System.now()
        isGpsTime = true

        if (isGpsTimedOut()) {
            Alerts.snackbar(host, strings.get(Res.string.no_gps_signal))
            gpsTime = Clock.System.now()
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
            now = Clock.System.now()
            delay(20)
        }
    }

    val systemDiff = (now - systemTime)
    val currentTime = gpsTime.plus(systemDiff)
    val myTime = currentTime.toLocalDateTime()
    val timeText = formatService.formatTime(myTime.time)
    val dateText = formatService.formatDate(myTime.date)
    val showAnalogClock = prefs.clock.showAnalogClock

    // One question: is this clock right?
    TrailSensePage(
        title = appString(Res.string.tool_clock_title),
        onBack = rememberTrailSenseBack(),
        actions = {
            // The instrumented test treats this as the toolbar's right button.
            IconButton(
                onClick = { startGpsUpdate() },
                modifier = Modifier.testTag("toolbar_right_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = appString(Res.string.reload)
                )
            }
        }
    ) {
        item("time") {
            // The drift between the device clock and GPS, shown on the face as a ghost second hand.
            val driftSeconds = if (isGpsTime && !isUpdatingClock) {
                (systemTime - gpsTime).inWholeMilliseconds / 1000f
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
                            .padding(bottom = TrailSenseSpacing.md)
                            .size(288.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnalogClock(time = myTime.time, driftSeconds = driftSeconds)
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
                val offset = getClockOffset((gpsTime - systemTime))
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
                            isUpdatingClock -> appString(Res.string.clock_waiting_for_gps)
                            isGpsTime -> clockOffsetLabel(systemTime, gpsTime)
                            else -> appString(Res.string.no_gps_signal)
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
                visible = !isUpdatingClock && onScheduleNextMinute != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
            ) {
                // Emphasis follows importance: a trivial drift gets a tonal button, a real one the filled primary.
                val drift = getClockOffset((gpsTime - systemTime)).seconds
                val onClick = {
                    requestExactAlarms {
                        onScheduleNextMinute?.invoke(gpsTime, systemTime)
                    }
                }
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = appString(Res.string.pip_button),
                    onClick = onClick,
                    style = if (isGpsTime && drift <= TRIVIAL_OFFSET_SECONDS) {
                        com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal
                    } else {
                        com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Filled
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pip_button")
                )
            }
        }
    }
}

/**
 * The face: four quarter marks (twelve larger), a short, thick hour hand and a longer, thinner minute hand, both in
 * the face's ink, and a thin second hand that is the face's one accent colour, stepping from mark to mark. Width and
 * length set the hierarchy, so the hands never read as the same value. When the device clock drifts from GPS, a faint
 * ghost second hand shows where the device thinks it is.
 */
@Composable
private fun AnalogClock(time: LocalTime, driftSeconds: Float) {
    val face = MaterialTheme.colorScheme.onPrimaryContainer
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
                // Lands on the mark without swinging past it (a bouncy tick overshot the face's marks).
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Canvas(modifier = Modifier.size(224.dp)) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Only the quarters, so the marks don't compete with the cookie's lobes; twelve is the larger one.
        for (quarter in 0 until 4) {
            val angle = quarter * 90f - 90f
            drawCircle(
                color = face.copy(alpha = if (quarter == 0) 0.9f else 0.6f),
                radius = if (quarter == 0) 6.dp.toPx() else 4.dp.toPx(),
                center = center.pointAt(radius * 0.92f, angle)
            )
        }

        val hour = time.hour % 12
        val hourAngle = ((hour + time.minute / 60f) / 12f) * 360f - 90f
        val minuteAngle = ((time.minute + time.second / 60f) / 60f) * 360f - 90f
        val secondAngle = secondHand.value - 90f

        drawLine(
            color = face.copy(alpha = 0.85f),
            start = center,
            end = center.pointAt(radius * 0.8f, minuteAngle),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = face,
            start = center,
            end = center.pointAt(radius * 0.5f, hourAngle),
            strokeWidth = 14.dp.toPx(),
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
            start = center,
            end = center.pointAt(radius * 0.8f, secondAngle),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(second, radius = 6.dp.toPx(), center = center.pointAt(radius * 0.8f, secondAngle))
        // The pivot in the accent too, so the second hand reads as one piece.
        drawCircle(second, radius = 12.dp.toPx(), center = center)
        drawCircle(container, radius = 4.dp.toPx(), center = center)
    }
}

private fun Offset.pointAt(radius: Float, degrees: Float): Offset {
    val radians = degrees.toDouble() * kotlin.math.PI / 180.0
    return Offset(
        x + (cos(radians) * radius).toFloat(),
        y + (sin(radians) * radius).toFloat()
    )
}

@Composable
private fun clockOffsetLabel(systemTime: Instant, gpsTime: Instant): String {
    val offset = getClockOffset((gpsTime - systemTime))
    val seconds = offset.seconds.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    return when (offset.direction) {
        ClockOffsetDirection.Accurate -> appString(Res.string.clock_device_time_accurate)
        ClockOffsetDirection.Slow -> appPluralString(
            Res.plurals.clock_device_time_slow,
            seconds,
            seconds
        )
        ClockOffsetDirection.Fast -> appPluralString(
            Res.plurals.clock_device_time_fast,
            seconds,
            seconds
        )
    }
}

/** A drift this small doesn't make the clock untrustworthy. */
private const val TRIVIAL_OFFSET_SECONDS = 2L

data class ClockOffset(val seconds: Long, val direction: ClockOffsetDirection)

enum class ClockOffsetDirection {
    Accurate,
    Slow,
    Fast
}

private fun getClockOffset(duration: Duration): ClockOffset {
    val seconds = kotlin.math.abs(duration.toJavaSeconds())
    val direction = when {
        seconds == 0L -> ClockOffsetDirection.Accurate
        duration.isNegative() -> ClockOffsetDirection.Fast
        else -> ClockOffsetDirection.Slow
    }
    return ClockOffset(seconds, direction)
}
