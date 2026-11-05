package com.kylecorry.trail_sense.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun UnitSettingsToolScreen(host: TrailSenseDestinationHost) {
    val cache = remember { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val userPrefs = remember { UserPreferences(host.requireContext()) }
    val hasBarometer = remember { Sensors.hasBarometer(host.requireContext()) }
    val distanceOptions = remember {
        host.settingsOptions(R.array.distance_unit_entries, R.array.distance_unit_values)
    }
    val pressureOptions = remember {
        host.settingsOptions(R.array.pressure_unit_entries, R.array.pressure_unit_values)
    }
    val temperatureOptions = remember {
        host.settingsOptions(R.array.temperature_unit_entries, R.array.temperature_unit_values)
    }
    val weightOptions = remember {
        host.settingsOptions(R.array.weight_entries, R.array.weight_values)
    }
    val coordinateOptions = remember {
        host.settingsOptions(R.array.coordinate_format_entries, R.array.coordinate_format_values)
    }
    val mapOptions = remember {
        host.settingsOptions(R.array.map_url_entries, R.array.map_url_values)
    }

    var distanceUnits by remember {
        mutableStateOf(distanceUnitsValue(userPrefs.distanceUnits))
    }
    var pressureUnits by remember {
        mutableStateOf(pressureUnitsValue(userPrefs.pressureUnits))
    }
    var temperatureUnits by remember {
        mutableStateOf(temperatureUnitsValue(userPrefs.temperatureUnits))
    }
    var weightUnits by remember {
        mutableStateOf(weightUnitsValue(userPrefs.weightUnits))
    }
    var coordinateFormat by remember {
        mutableStateOf(coordinateFormatValue(userPrefs.coordinateFormat))
    }
    var mapSite by remember {
        mutableStateOf(mapSiteValue(userPrefs.mapSite))
    }
    var use24HourTime by remember { mutableStateOf(userPrefs.use24HourTime) }
    var includeLeadingZero by remember {
        mutableStateOf(userPrefs.addLeadingZeroToTime)
    }

    val measurementsTitle = stringResource(R.string.settings_section_measurements)
    val fineTuneTitle = stringResource(R.string.units_fine_tune)
    val locationTitle = stringResource(R.string.location)
    val timeTitle = stringResource(R.string.time)
    val timeExample = stringResource(
        R.string.settings_time_format_example,
        exampleTime(use24HourTime, includeLeadingZero)
    )

    SettingsList(host, title = stringResource(R.string.units)) {
        // The one question most people have: metric or imperial. One tap sets every unit; the rows below are
        // for the few who mix them (metres with °F), each showing its current value.
        val metric = listOf("meters", "c", "kg") + if (hasBarometer) listOf("hpa") else emptyList()
        val imperial = listOf("feet_miles", "f", "lbs") + if (hasBarometer) listOf("in") else emptyList()
        val current = listOf(distanceUnits, temperatureUnits, weightUnits) + if (hasBarometer) listOf(pressureUnits) else emptyList()
        fun applySystem(values: List<String>) {
            distanceUnits = values[0]; host.putString(cache, R.string.pref_distance_units, values[0])
            temperatureUnits = values[1]; host.putString(cache, R.string.pref_temperature_units, values[1])
            weightUnits = values[2]; host.putString(cache, R.string.pref_weight_units, values[2])
            if (hasBarometer) {
                pressureUnits = values[3]; host.putString(cache, R.string.pref_pressure_units, values[3])
            }
        }
        item("system") {
            UnitSystemCards(
                metricSelected = current == metric,
                imperialSelected = current == imperial,
                onMetric = { applySystem(metric) },
                onImperial = { applySystem(imperial) }
            )
        }
        trailSenseSection("fine_tune", title = fineTuneTitle) {
            row { position ->
                UnitValueRow(host, stringResource(R.string.distance), distanceOptions, distanceUnits, position) {
                    distanceUnits = it; host.putString(cache, R.string.pref_distance_units, it)
                }
            }
            row { position ->
                UnitValueRow(host, stringResource(R.string.temperature), temperatureOptions, temperatureUnits, position) {
                    temperatureUnits = it; host.putString(cache, R.string.pref_temperature_units, it)
                }
            }
            row { position ->
                UnitValueRow(host, stringResource(R.string.weight), weightOptions, weightUnits, position) {
                    weightUnits = it; host.putString(cache, R.string.pref_weight_units, it)
                }
            }
            if (hasBarometer) {
                row { position ->
                    UnitValueRow(host, stringResource(R.string.pressure), pressureOptions, pressureUnits, position) {
                        pressureUnits = it; host.putString(cache, R.string.pref_pressure_units, it)
                    }
                }
            }
        }
        trailSenseSection("location", title = locationTitle) {
            row { position ->
                UnitValueRow(host, stringResource(R.string.pref_coordinate_format_title), coordinateOptions, coordinateFormat, position) {
                    coordinateFormat = it
                    host.putString(cache, R.string.pref_coordinate_format, it)
                }
            }
            row { position ->
                UnitValueRow(host, stringResource(R.string.pref_map_url_source_title), mapOptions, mapSite, position) {
                    mapSite = it
                    host.putString(cache, R.string.pref_map_url_source, it)
                }
            }
        }
        trailSenseSection("time", title = timeTitle, footer = timeExample) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_use_24_hour_title),
                    checked = use24HourTime,
                    onCheckedChange = {
                        use24HourTime = it
                        host.putBoolean(cache, R.string.pref_use_24_hour, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_include_leading_zero_title),
                    checked = includeLeadingZero,
                    onCheckedChange = {
                        includeLeadingZero = it
                        host.putBoolean(cache, R.string.pref_include_leading_zero, it)
                    },
                    position = position
                )
            }
        }
    }
}

/**
 * Metric or Imperial as two big cards with what each looks like (km · °C · kg); the chosen one fills with the
 * primary container and a check. With mixed units, neither is chosen.
 */
@Composable
private fun UnitSystemCards(
    metricSelected: Boolean,
    imperialSelected: Boolean,
    onMetric: () -> Unit,
    onImperial: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = TrailSenseSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        UnitSystemCard(stringResource(R.string.units_metric), stringResource(R.string.units_metric_example), metricSelected, onMetric, Modifier.weight(1f))
        UnitSystemCard(stringResource(R.string.units_imperial), stringResource(R.string.units_imperial_example), imperialSelected, onImperial, Modifier.weight(1f))
    }
}

@Composable
private fun UnitSystemCard(title: String, example: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainer, label = "unitCard"
    )
    val content = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = content
    ) {
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    example,
                    style = MaterialTheme.typography.bodyLarge,
                    color = content.copy(alpha = 0.75f)
                )
            }
            // A radio-style mark at the top start: filled with a check when chosen, an empty ring otherwise.
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .then(
                        if (selected) Modifier.background(scheme.primary, CircleShape)
                        else Modifier.border(2.dp, scheme.outline, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** How a time reads with these choices, shown under them so the switches explain themselves. */
private fun exampleTime(use24HourTime: Boolean, includeLeadingZero: Boolean): String {
    val hour = if (use24HourTime) "H" else "h"
    val pattern = (if (includeLeadingZero) hour + hour else hour) +
            ":mm" + if (use24HourTime) "" else " a"
    return LocalTime.of(9, 5).format(DateTimeFormatter.ofPattern(pattern))
}

private fun distanceUnitsValue(units: UserPreferences.DistanceUnits): String {
    return when (units) {
        UserPreferences.DistanceUnits.Meters -> "meters"
        UserPreferences.DistanceUnits.Feet -> "feet_miles"
    }
}

private fun pressureUnitsValue(units: PressureUnits): String {
    return when (units) {
        PressureUnits.Hpa -> "hpa"
        PressureUnits.Inhg -> "in"
        PressureUnits.MmHg -> "mm"
        PressureUnits.Mbar -> "mbar"
        PressureUnits.Psi -> "psi"
    }
}

private fun temperatureUnitsValue(units: TemperatureUnits): String {
    return when (units) {
        TemperatureUnits.Celsius -> "c"
        TemperatureUnits.Fahrenheit -> "f"
    }
}

private fun weightUnitsValue(units: WeightUnits): String {
    return when (units) {
        WeightUnits.Kilograms -> "kg"
        WeightUnits.Pounds -> "lbs"
        WeightUnits.Stone -> "stone"
        else -> "kg"
    }
}

private fun coordinateFormatValue(format: BuiltInCoordinateFormat): String {
    return when (format) {
        BuiltInCoordinateFormat.DecimalDegrees -> "dd"
        BuiltInCoordinateFormat.DegreesDecimalMinutes -> "ddm"
        BuiltInCoordinateFormat.DegreesMinutesSeconds -> "dms"
        BuiltInCoordinateFormat.UTM -> "utm"
        BuiltInCoordinateFormat.MGRS -> "mgrs"
        BuiltInCoordinateFormat.USNG -> "usng"
        BuiltInCoordinateFormat.OSGB -> "osng"
    }
}

private fun mapSiteValue(site: MapSite): String {
    return when (site) {
        MapSite.Apple -> "apple"
        MapSite.Bing -> "bing"
        MapSite.Caltopo -> "caltopo"
        MapSite.Google -> "google"
        MapSite.OSM -> "osm"
    }
}
