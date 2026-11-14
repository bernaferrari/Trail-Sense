package com.kylecorry.trail_sense.shared.views

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.tools.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.StatusBadge

class SensorStatusBadgeView(
    private val context: Context
) {

    private var gpsBadge by mutableStateOf<StatusBadge?>(null)
    private var compassBadge by mutableStateOf<StatusBadge?>(null)
    private var showGpsBadge by mutableStateOf(false)
    private var showCompassBadge by mutableStateOf(getAppService<SensorService>().hasCompass())

    private var gpsStatusBadgeProvider: GpsStatusBadgeProvider? = null
    private var compassStatusBadgeProvider: SensorStatusBadgeProvider? = null

    private var gps: ISatelliteGPS? = null
    private var compassSensor: Sensor? = null

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
    fun Content(modifier: Modifier = Modifier, quiet: Boolean = false) {
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
        val items = listOfNotNull(
            gpsBadge?.takeIf { showGpsBadge }?.let {
                SensorStatusItem(it, context.getString(R.string.gps), TrailSenseIcons.Filled.LocationOn)
            },
            compassBadge?.takeIf { showCompassBadge }?.let {
                SensorStatusItem(it, context.getString(R.string.pref_compass_sensor_title), TrailSenseIcons.Filled.Explore)
            }
        )
        SensorStatusContent(items, quiet, alphaState, clickListener, modifier)
    }

    fun setSensors(gps: ISatelliteGPS?, compass: Sensor) {
        showGpsBadge = gps != null
        this.gps = gps
        this.compassSensor = compass
        gpsStatusBadgeProvider = gps?.let { GpsStatusBadgeProvider(it, context) }
        compassStatusBadgeProvider = SensorStatusBadgeProvider(compass, TrailIcons.CompassIcon)
        // Immediately update so badges don't show stale data
        updateBadges()
    }

    fun getSensors(): List<Sensor> {
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
