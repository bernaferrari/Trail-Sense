package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import androidx.compose.foundation.shape.CircleShape
import com.kylecorry.trail_sense.shared.colors.AppColor
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.StatusBadge
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider

class SensorStatusBadgeView(
    private val context: Context
) {

    private var gpsBadge by mutableStateOf<StatusBadge?>(null)
    private var compassBadge by mutableStateOf<StatusBadge?>(null)
    private var showGpsBadge by mutableStateOf(false)
    private var showCompassBadge by mutableStateOf(SensorService(context).hasCompass())

    private var gpsStatusBadgeProvider: GpsStatusBadgeProvider? = null
    private var compassStatusBadgeProvider: SensorStatusBadgeProvider? = null

    private var gps: ISatelliteGPS? = null
    private var compassSensor: ISensor? = null

    private var isStarted = false
    private var badgeVisible by mutableStateOf(true)
    private var alphaState by mutableStateOf(1f)
    private var clickListener by mutableStateOf<(() -> Unit)?>(null)

    private val updateTimer = CoroutineTimer {
        updateBadges()
    }

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> start()
            Lifecycle.Event.ON_PAUSE -> stop()
            else -> {}
        }
    }

    var alpha: Float
        get() = alphaState
        set(value) {
            alphaState = value
        }

    var isVisible: Boolean
        get() = badgeVisible
        set(value) {
            badgeVisible = value
        }

    fun setOnClickListener(listener: (() -> Unit)?) {
        clickListener = listener
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.addObserver(lifecycleListener)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleListener)
                stop()
            }
        }
        if (!badgeVisible) {
            return
        }
        Column(
            modifier = modifier
                .alpha(alphaState)
                .clickable(enabled = clickListener != null) { clickListener?.invoke() },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            val gps = gpsBadge
            val compass = compassBadge
            if (showGpsBadge && gps != null && !gps.isGood()) {
                SensorBadgeContent(
                    badge = gps,
                    label = context.getString(R.string.gps),
                    icon = TrailSenseIcons.Filled.LocationOn
                )
            }
            if (showCompassBadge && compass != null && !compass.isGood()) {
                SensorBadgeContent(
                    badge = compass,
                    label = context.getString(R.string.pref_compass_sensor_title),
                    icon = TrailSenseIcons.Filled.Explore
                )
            }
        }
    }

    fun setSensors(gps: ISatelliteGPS?, compass: ISensor) {
        showGpsBadge = gps != null
        this.gps = gps
        this.compassSensor = compass
        gpsStatusBadgeProvider = gps?.let { GpsStatusBadgeProvider(it, context) }
        compassStatusBadgeProvider = SensorStatusBadgeProvider(compass, context, TrailIcons.CompassIcon)
        // Immediately update so badges don't show stale data
        updateBadges()
    }

    fun getSensors(): List<ISensor> {
        return listOfNotNull(gps, compassSensor)
    }

    private fun start() {
        if (isStarted) return
        isStarted = true
        updateTimer.interval(1000)
    }

    private fun stop() {
        if (!isStarted) return
        isStarted = false
        updateTimer.stop()
    }

    private fun updateBadges() {
        gpsBadge = gpsStatusBadgeProvider?.getBadge()
        compassBadge = compassStatusBadgeProvider?.getBadge()
    }
}

@Composable
private fun SensorBadgeContent(
    badge: StatusBadge,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    // The badge only appears when something needs attention: amber while searching or for a fair signal, the
    // error tone when it's poor or unavailable.
    val status = trailSenseStatusColors()
    val caution = badge.color == AppColor.Yellow.color || badge.color == AppColor.Orange.color
    Surface(
        modifier = modifier,
        color = if (caution) status.cautionContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (caution) status.onCautionContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$label · ${badge.name}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** A good reading needs no badge: healthy, or reported in the quality scale's green. */
private fun StatusBadge.isGood(): Boolean = isHealthy || color == AppColor.Green.color
