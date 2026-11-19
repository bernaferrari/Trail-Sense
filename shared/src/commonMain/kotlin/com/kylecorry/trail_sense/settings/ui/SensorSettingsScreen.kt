package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import org.jetbrains.compose.resources.StringResource
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun SensorSettingsToolScreen(showCompass: Boolean = true) {
    val navigator = LocalNavigator.current
    val hasBarometer = remember { getAppService<DeviceProfile>().hasBarometer }
    val items = remember(hasBarometer, showCompass) {
        sensorSettingsItems(hasBarometer, showCompass)
    }
    // The sensors to calibrate, then the raw readings of every sensor on their own.
    val (details, sensors) = items.partition { it.key == "details" }

    SettingsList(title = appString(Res.string.sensors)) {
        listOf("sensors" to sensors, "details" to details).forEach { (key, group) ->
            trailSenseSection(key) {
                group.forEach { item ->
                    row { position ->
                        TrailSenseListItem(
                            title = appString(item.title),
                            icon = rememberVectorPainter(item.icon),
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                navigator.navigate(item.destination)
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

private fun sensorSettingsItems(hasBarometer: Boolean, showCompass: Boolean): List<SensorSettingsItem> {
    return listOfNotNull(
        if (showCompass) SensorSettingsItem(
            key = "compass",
            title = Res.string.pref_compass_sensor_title,
            icon = TrailIcons.CompassIcon,
            destination = Route.CalibrateCompass
        ) else null,
        SensorSettingsItem(
            key = "gps",
            title = Res.string.gps,
            icon = TrailIcons.Satellite,
            destination = Route.CalibrateGps
        ),
        SensorSettingsItem(
            key = "altimeter",
            title = Res.string.pref_altimeter_calibration_title,
            icon = TrailIcons.Altitude,
            destination = Route.CalibrateAltimeter
        ),
        if (hasBarometer) {
            SensorSettingsItem(
                key = "barometer",
                title = Res.string.barometer,
                icon = TrailIcons.Barometer,
                destination = Route.CalibrateBarometer
            )
        } else {
            null
        },
        SensorSettingsItem(
            key = "temperature",
            title = Res.string.tool_thermometer_title,
            icon = TrailIcons.Thermometer,
            destination = Route.ThermometerSettings
        ),
        SensorSettingsItem(
            key = "camera",
            title = Res.string.camera,
            icon = TrailIcons.Camera,
            destination = Route.CameraSettings
        ),
        SensorSettingsItem(
            key = "cell_signal",
            title = Res.string.cell_signal,
            icon = TrailIcons.SignalCellular3,
            destination = Route.CellSignalSettings
        ),
        SensorSettingsItem(
            key = "details",
            title = Res.string.pref_sensor_details_title,
            icon = TrailIcons.Sensors,
            destination = Route.SensorDetails
        )
    )
}

private data class SensorSettingsItem(
    val key: String,
    val title: StringResource,
    val icon: ImageVector,
    val destination: Route
)
