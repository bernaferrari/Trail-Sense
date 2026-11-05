package com.kylecorry.trail_sense.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.Altitude
import com.kylecorry.trail_sense.shared.icons.trail.Barometer
import com.kylecorry.trail_sense.shared.icons.trail.Camera
import com.kylecorry.trail_sense.shared.icons.trail.CompassIcon
import com.kylecorry.trail_sense.shared.icons.trail.Satellite
import com.kylecorry.trail_sense.shared.icons.trail.Sensors
import com.kylecorry.trail_sense.shared.icons.trail.SignalCellular3
import com.kylecorry.trail_sense.shared.icons.trail.Thermometer
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun SensorSettingsToolScreen(host: TrailSenseDestinationHost) {
    val hasBarometer = remember { Sensors.hasBarometer(host.requireContext()) }
    val items = remember(hasBarometer) {
        sensorSettingsItems(hasBarometer)
    }
    // The sensors to calibrate, then the raw readings of every sensor on their own.
    val (details, sensors) = items.partition { it.key == "details" }

    SettingsList(host, title = stringResource(R.string.sensors)) {
        listOf("sensors" to sensors, "details" to details).forEach { (key, group) ->
            trailSenseSection(key) {
                group.forEach { item ->
                    row { position ->
                        TrailSenseListItem(
                            title = stringResource(item.title),
                            icon = rememberVectorPainter(item.icon),
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                host.findNavigator().navigate(item.destination)
                            },
                            position = position,
                            animateEntrance = false
                        )
                    }
                }
            }
        }
    }
}

private fun sensorSettingsItems(hasBarometer: Boolean): List<SensorSettingsItem> {
    return listOfNotNull(
        SensorSettingsItem(
            key = "compass",
            title = R.string.pref_compass_sensor_title,
            icon = TrailIcons.CompassIcon,
            destination = Route.CalibrateCompass
        ),
        SensorSettingsItem(
            key = "gps",
            title = R.string.gps,
            icon = TrailIcons.Satellite,
            destination = Route.CalibrateGps
        ),
        SensorSettingsItem(
            key = "altimeter",
            title = R.string.pref_altimeter_calibration_title,
            icon = TrailIcons.Altitude,
            destination = Route.CalibrateAltimeter
        ),
        if (hasBarometer) {
            SensorSettingsItem(
                key = "barometer",
                title = R.string.barometer,
                icon = TrailIcons.Barometer,
                destination = Route.CalibrateBarometer
            )
        } else {
            null
        },
        SensorSettingsItem(
            key = "temperature",
            title = R.string.tool_thermometer_title,
            icon = TrailIcons.Thermometer,
            destination = Route.ThermometerSettings
        ),
        SensorSettingsItem(
            key = "camera",
            title = R.string.camera,
            icon = TrailIcons.Camera,
            destination = Route.CameraSettings
        ),
        SensorSettingsItem(
            key = "cell_signal",
            title = R.string.cell_signal,
            icon = TrailIcons.SignalCellular3,
            destination = Route.CellSignalSettings
        ),
        SensorSettingsItem(
            key = "details",
            title = R.string.pref_sensor_details_title,
            icon = TrailIcons.Sensors,
            destination = Route.SensorDetails
        )
    )
}

private data class SensorSettingsItem(
    val key: String,
    val title: Int,
    val icon: ImageVector,
    val destination: Route
)
