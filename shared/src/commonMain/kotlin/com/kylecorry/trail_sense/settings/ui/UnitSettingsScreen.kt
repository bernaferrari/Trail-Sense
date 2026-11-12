package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString


import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.device.DeviceProfile
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
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format
import kotlinx.datetime.format.char

@Composable
fun UnitSettingsToolScreen() {
    val cache = remember { getAppService<Preferences>() }
    val userPrefs = remember { getAppService<UserPreferences>() }
    val hasBarometer = remember { getAppService<DeviceProfile>().hasBarometer }
    val distanceOptions = rememberSettingsOptions(Res.array.distance_unit_entries, Res.array.distance_unit_values)
    val pressureOptions = rememberSettingsOptions(Res.array.pressure_unit_entries, Res.array.pressure_unit_values)
    val temperatureOptions = rememberSettingsOptions(Res.array.temperature_unit_entries, Res.array.temperature_unit_values)
    val weightOptions = rememberSettingsOptions(Res.array.weight_entries, Res.array.weight_values)
    val coordinateOptions = rememberSettingsOptions(Res.array.coordinate_format_entries, Res.array.coordinate_format_values)
    val mapOptions = rememberSettingsOptions(Res.array.map_url_entries, Res.array.map_url_values)

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

    val measurementsTitle = appString(Res.string.settings_section_measurements)
    val fineTuneTitle = appString(Res.string.units_fine_tune)
    val locationTitle = appString(Res.string.location)
    val timeTitle = appString(Res.string.time)
    val timeExample = appString(Res.string.settings_time_format_example,
        exampleTime(use24HourTime, includeLeadingZero)
    )

    SettingsList(title = appString(Res.string.units)) {
        // The one question most people have: metric or imperial. One tap sets every unit; the rows below are
        // for the few who mix them (metres with °F), each showing its current value.
        val metric = listOf("meters", "c", "kg") + if (hasBarometer) listOf("hpa") else emptyList()
        val imperial = listOf("feet_miles", "f", "lbs") + if (hasBarometer) listOf("in") else emptyList()
        val current = listOf(distanceUnits, temperatureUnits, weightUnits) + if (hasBarometer) listOf(pressureUnits) else emptyList()
        fun applySystem(values: List<String>) {
            distanceUnits = values[0]; cache.putString(PreferenceKeys.distanceUnits, values[0])
            temperatureUnits = values[1]; cache.putString(PreferenceKeys.temperatureUnits, values[1])
            weightUnits = values[2]; cache.putString(PreferenceKeys.weightUnits, values[2])
            if (hasBarometer) {
                pressureUnits = values[3]; cache.putString(PreferenceKeys.pressureUnits, values[3])
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
                UnitValueRow(appString(Res.string.distance), distanceOptions, distanceUnits, position) {
                    distanceUnits = it; cache.putString(PreferenceKeys.distanceUnits, it)
                }
            }
            row { position ->
                UnitValueRow(appString(Res.string.temperature), temperatureOptions, temperatureUnits, position) {
                    temperatureUnits = it; cache.putString(PreferenceKeys.temperatureUnits, it)
                }
            }
            row { position ->
                UnitValueRow(appString(Res.string.weight), weightOptions, weightUnits, position) {
                    weightUnits = it; cache.putString(PreferenceKeys.weightUnits, it)
                }
            }
            if (hasBarometer) {
                row { position ->
                    UnitValueRow(appString(Res.string.pressure), pressureOptions, pressureUnits, position) {
                        pressureUnits = it; cache.putString(PreferenceKeys.pressureUnits, it)
                    }
                }
            }
        }
        trailSenseSection("location", title = locationTitle) {
            row { position ->
                UnitValueRow(appString(Res.string.pref_coordinate_format_title), coordinateOptions, coordinateFormat, position) {
                    coordinateFormat = it
                    cache.putString(PreferenceKeys.coordinateFormat, it)
                }
            }
            row { position ->
                UnitValueRow(appString(Res.string.pref_map_url_source_title), mapOptions, mapSite, position) {
                    mapSite = it
                    cache.putString(PreferenceKeys.mapUrlSource, it)
                }
            }
        }
        trailSenseSection("time", title = timeTitle, footer = timeExample) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_use_24_hour_title),
                    checked = use24HourTime,
                    onCheckedChange = {
                        use24HourTime = it
                        cache.putBoolean(PreferenceKeys.use24Hour, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_include_leading_zero_title),
                    checked = includeLeadingZero,
                    onCheckedChange = {
                        includeLeadingZero = it
                        cache.putBoolean(PreferenceKeys.includeLeadingZero, it)
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
        UnitSystemCard(appString(Res.string.units_metric), appString(Res.string.units_metric_example), metricSelected, onMetric, Modifier.weight(1f))
        UnitSystemCard(appString(Res.string.units_imperial), appString(Res.string.units_imperial_example), imperialSelected, onImperial, Modifier.weight(1f))
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
    val padding = if (includeLeadingZero) Padding.ZERO else Padding.NONE
    val format = LocalTime.Format {
        if (use24HourTime) hour(padding) else amPmHour(padding)
        char(':')
        minute()
        if (!use24HourTime) {
            char(' ')
            amPmMarker("AM", "PM")
        }
    }
    return LocalTime(9, 5).format(format)
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
