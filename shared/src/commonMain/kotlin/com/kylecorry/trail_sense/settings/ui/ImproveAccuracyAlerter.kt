package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.dialogs.showComposeDialog
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * How to improve the GPS and compass: the GPS's accuracy and satellites, the compass calibration steps with the
 * figure-8 animation, and an optional [customMessage]. All drawn in Compose in the app's dialog.
 */
class ImproveAccuracyAlerter(
    private val host: AppHost = AppHost.current(),
    private val customMessage: CharSequence? = null
) : IValueAlerter<List<Sensor>> {

    private val formatter = FormatService.getInstance()
    private val baseDistanceUnits = getAppService<UserPreferences>().baseDistanceUnits
    private val titleScale = 1.2f
    private val doneButton = getAppService<AppStrings>().system(SystemString.Ok)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun alert(value: List<Sensor>) {
        val gps = value.firstOrNull { it is ISatelliteGPS } as? ISatelliteGPS
        val compass = (value.firstOrNull { it is CompassSensor } as? CompassSensor)
            ?: value.firstOrNull { it is Magnetometer }
        val orientation = value.firstOrNull { it is OrientationSensor } as? OrientationSensor
        val hasCompass = compass !is MockSensor
        val content = buildAnnotatedString {
            gps?.let {
                appendGPS(it)
                append("\n\n")
            }
            compass?.let {
                appendCompass(it)
            }
            orientation?.let {
                appendCompass(it)
            }
            customMessage?.let {
                append("\n\n")
                append(it.toString())
            }
        }
        val title = getAppService<AppStrings>().get(Res.string.accuracy_info_title)

        showComposeDialog(host, cancelOnOutsideTouch = false) { dismiss ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.xl)
                    .padding(bottom = TrailSenseSpacing.lg)
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasCompass) {
                    CompassCalibrationContent(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm, Alignment.End)
            ) {
                Button(onClick = dismiss, shapes = ButtonDefaults.shapes()) {
                    Text(doneButton)
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendCompass(compass: Sensor) {
        if (compass is MockSensor) {
            return
        }

        title(getAppService<AppStrings>().get(Res.string.pref_compass_sensor_title))

        appendLine(getAppService<AppStrings>().get(Res.string.calibrate_compass_dialog_content, doneButton))
    }

    private fun AnnotatedString.Builder.appendGPS(gps: ISatelliteGPS) {
        if (gps is InactiveGPS) {
            return
        }

        title(getAppService<AppStrings>().get(Res.string.gps))

        appendLine(getAppService<AppStrings>().get(Res.string.gps_accuracy_tip))
        appendLine()

        // Horizontal accuracy
        gps.horizontalAccuracy?.let {
            val accuracyText = getAppService<AppStrings>().get(Res.string.accuracy_distance_format,
                formatter.formatDistance(Distance.meters(it).convertTo(baseDistanceUnits))
            )
            bold(getAppService<AppStrings>().get(Res.string.gps_location_accuracy) + ": ")
            appendLine(accuracyText)
        }

        // Vertical accuracy
        gps.verticalAccuracy?.let {
            val accuracyText = getAppService<AppStrings>().get(Res.string.accuracy_distance_format,
                formatter.formatDistance(Distance.meters(it).convertTo(baseDistanceUnits))
            )
            bold(getAppService<AppStrings>().get(Res.string.gps_elevation_accuracy) + ": ")
            appendLine(accuracyText)
        }

        // Satellites
        bold(getAppService<AppStrings>().get(Res.string.gps_satellites) + ": ")
        appendLine((gps.satellites ?: 0).toString())
    }

    private fun AnnotatedString.Builder.bold(text: String) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text)
        }
    }

    private fun AnnotatedString.Builder.title(title: String) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = titleScale.em)) {
            appendLine(title)
        }
    }

}
