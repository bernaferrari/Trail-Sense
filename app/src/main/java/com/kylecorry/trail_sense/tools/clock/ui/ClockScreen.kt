@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.clock.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.time.toJavaSeconds
import com.kylecorry.trail_sense.shared.time.toInstant
import com.kylecorry.trail_sense.shared.time.minus
import com.kylecorry.trail_sense.shared.time.plus
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock
import kotlin.time.toKotlinInstant

import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.Alert
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
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
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
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.snackbar

@Composable
fun ClockToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val formatService = remember { FormatService.getInstance(context) }
    val sensorService = remember { getAppService<SensorService>() }
    val gps = remember { sensorService.getGPS() }
    val prefs = remember { UserPreferences(context) }

    ClockScreen(
        formatService = formatService,
        prefs = prefs,
        gps = gps,
        isGpsTimedOut = { gps is CustomGPS && gps.isTimedOut },
        getGpsTime = { getGPSTime(gps) },
        requestExactAlarms = { onResult ->
            scope.launch { onResult(effects.requestPermission(AppPermission.ExactAlarms)) }
        },
        onScheduleNextMinute = { gpsTime, systemTime ->
            sendNextMinuteNotification(context, formatService, gpsTime, systemTime)
        }
    )
}

private fun getGPSTime(gps: ISatelliteGPS): Instant {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        tryOrNothing {
            return SystemClock.currentGnssTimeClock().instant().toKotlinInstant()
        }
    }

    if (gps.eventTimeElapsedNanos == 0L) {
        return gps.timestamp
    }
    val systemTime = SystemClock.elapsedRealtimeNanos()
    val timeDiff = gps.eventTimeElapsedNanos - systemTime
    return gps.timestamp + timeDiff.nanoseconds
}

private fun sendNextMinuteNotification(
    context: Context,
    formatService: FormatService,
    gpsTime: Instant,
    systemTime: Instant
) {
    val systemDiff = (Clock.System.now() - systemTime)
    val currentTime = gpsTime.plus(systemDiff)
    val clockError = (gpsTime - systemTime)
    // Wall-clock arithmetic in the system zone, as before: the next whole local minute (skipping one when
    // fewer than 15 seconds remain), shifted by the clock error and resolved back to an instant.
    val displayTimeWithSeconds = currentTime.toLocalDateTime()
    val minuteStart = LocalDateTime(
        displayTimeWithSeconds.date,
        LocalTime(displayTimeWithSeconds.hour, displayTimeWithSeconds.minute)
    )
    val displayTime = if (displayTimeWithSeconds.second > 45) {
        minuteStart + 2.minutes
    } else {
        minuteStart + 1.minutes
    }
    val sendTime = (displayTime - clockError).toInstant()
    val formattedTime = formatService.formatTime(displayTime.time)

    Alerts.dialog(
        context,
        context.getString(R.string.clock_sync_time_settings),
        context.getString(R.string.clock_sync_instructions, formattedTime)
    ) { cancelled ->
        if (!cancelled) {
            Alerts.snackbar(
                context,
                context.getString(R.string.pip_notification_scheduled, formattedTime)
            )

            val scheduler = AlarmTaskScheduler(context) {
                NextMinuteBroadcastReceiver.pendingIntent(context, formattedTime)
            }
            scheduler.once(sendTime)
            context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
    }
}
