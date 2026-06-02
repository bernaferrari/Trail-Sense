package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class UnitSettingsFragment : Fragment() {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val userPrefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    val hasBarometer = remember { Sensors.hasBarometer(requireContext()) }
                    val distanceOptions = remember {
                        options(R.array.distance_unit_entries, R.array.distance_unit_values)
                    }
                    val pressureOptions = remember {
                        options(R.array.pressure_unit_entries, R.array.pressure_unit_values)
                    }
                    val temperatureOptions = remember {
                        options(
                            R.array.temperature_unit_entries,
                            R.array.temperature_unit_values
                        )
                    }
                    val weightOptions = remember {
                        options(R.array.weight_entries, R.array.weight_values)
                    }
                    val coordinateOptions = remember {
                        options(
                            R.array.coordinate_format_entries,
                            R.array.coordinate_format_values
                        )
                    }
                    val mapOptions = remember {
                        options(R.array.map_url_entries, R.array.map_url_values)
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
                        mutableStateOf(coordinateFormatValue(userPrefs.navigation.coordinateFormat))
                    }
                    var mapSite by remember {
                        mutableStateOf(mapSiteValue(userPrefs.mapSite))
                    }
                    var use24HourTime by remember {
                        mutableStateOf(userPrefs.use24HourTime)
                    }
                    var includeLeadingZero by remember {
                        mutableStateOf(userPrefs.addLeadingZeroToTime)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.units))
                            }
                            item("distance") {
                                UnitPickerRow(
                                    label = stringResource(R.string.distance),
                                    options = distanceOptions,
                                    selected = distanceUnits,
                                    onSelected = {
                                        distanceUnits = it
                                        putString(R.string.pref_distance_units, it)
                                    }
                                )
                            }
                            if (hasBarometer) {
                                item("pressure") {
                                    UnitPickerRow(
                                        label = stringResource(R.string.pressure),
                                        options = pressureOptions,
                                        selected = pressureUnits,
                                        onSelected = {
                                            pressureUnits = it
                                            putString(R.string.pref_pressure_units, it)
                                        }
                                    )
                                }
                            }
                            item("temperature") {
                                UnitPickerRow(
                                    label = stringResource(R.string.temperature),
                                    options = temperatureOptions,
                                    selected = temperatureUnits,
                                    onSelected = {
                                        temperatureUnits = it
                                        putString(R.string.pref_temperature_units, it)
                                    }
                                )
                            }
                            item("weight") {
                                UnitPickerRow(
                                    label = stringResource(R.string.weight),
                                    options = weightOptions,
                                    selected = weightUnits,
                                    onSelected = {
                                        weightUnits = it
                                        putString(R.string.pref_weight_units, it)
                                    }
                                )
                            }
                            item("coordinate") {
                                UnitPickerRow(
                                    label = stringResource(R.string.pref_coordinate_format_title),
                                    options = coordinateOptions,
                                    selected = coordinateFormat,
                                    onSelected = {
                                        coordinateFormat = it
                                        putString(R.string.pref_coordinate_format, it)
                                    }
                                )
                            }
                            item("map_url") {
                                UnitPickerRow(
                                    label = stringResource(R.string.pref_map_url_source_title),
                                    options = mapOptions,
                                    selected = mapSite,
                                    onSelected = {
                                        mapSite = it
                                        putString(R.string.pref_map_url_source, it)
                                    }
                                )
                            }
                            item("use_24_hour") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_use_24_hour_title),
                                    checked = use24HourTime,
                                    onCheckedChange = {
                                        use24HourTime = it
                                        putBoolean(R.string.pref_use_24_hour, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("leading_zero") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_include_leading_zero_title),
                                    checked = includeLeadingZero,
                                    onCheckedChange = {
                                        includeLeadingZero = it
                                        putBoolean(R.string.pref_include_leading_zero, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun UnitPickerRow(
        label: String,
        options: List<UnitOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        SettingsPickerRow(
            label = label,
            selected = options.firstOrNull { it.value == selected }?.label ?: selected,
            onClick = {
                pickOption(
                    label,
                    options,
                    selected,
                    onSelected
                )
            },
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
        )
    }

    private fun pickOption(
        title: String,
        options: List<UnitOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun options(entriesId: Int, valuesId: Int): List<UnitOption> {
        val entries = resources.getStringArray(entriesId)
        val values = resources.getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            UnitOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
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

    private data class UnitOption(
        val value: String,
        val label: String
    )
}
