package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.StatusBadge
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider

class SensorStatusBadgeView(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    private var gpsBadge by mutableStateOf<StatusBadge?>(null)
    private var compassBadge by mutableStateOf<StatusBadge?>(null)
    private var showGpsBadge by mutableStateOf(false)
    private var showCompassBadge by mutableStateOf(SensorService(context).hasCompass())

    private var gpsStatusBadgeProvider: GpsStatusBadgeProvider? = null
    private var compassStatusBadgeProvider: SensorStatusBadgeProvider? = null

    private var gps: ISatelliteGPS? = null
    private var compassSensor: ISensor? = null

    private var isStarted = false

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

    init {
        doOnAttach {
            findViewTreeLifecycleOwner()?.lifecycle?.addObserver(lifecycleListener)
        }

        doOnDetach {
            findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(lifecycleListener)
            stop()
        }
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Column(
                modifier = Modifier.clickable { performClick() },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val gps = gpsBadge
                val compass = compassBadge
                if (showGpsBadge && gps != null) {
                    SensorBadgeContent(gps, modifier = Modifier)
                }
                if (showCompassBadge && compass != null) {
                    SensorBadgeContent(compass, modifier = Modifier)
                }
            }
        }
    }

    fun setSensors(gps: ISatelliteGPS?, compass: ISensor) {
        showGpsBadge = gps != null
        this.gps = gps
        this.compassSensor = compass
        gpsStatusBadgeProvider = gps?.let { GpsStatusBadgeProvider(it, context) }
        compassStatusBadgeProvider = SensorStatusBadgeProvider(compass, context, R.drawable.ic_compass_icon)
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(badge.color),
        contentColor = Color.Black,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(badge.icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
