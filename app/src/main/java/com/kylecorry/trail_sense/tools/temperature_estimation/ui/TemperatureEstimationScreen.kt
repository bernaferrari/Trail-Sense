@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.launch
import java.time.Duration

private enum class WindSpeedUnit(val distance: DistanceUnits, val time: TimeUnits) {
    KilometersPerHour(DistanceUnits.Kilometers, TimeUnits.Hours),
    MilesPerHour(DistanceUnits.Miles, TimeUnits.Hours),
    MetersPerSecond(DistanceUnits.Meters, TimeUnits.Seconds)
}

@Composable
fun TemperatureEstimationToolScreen() {
    val context = LocalContext.current
    val sensorService = remember { SensorService(context) }
    val thermometer = remember { sensorService.getThermometer() }
    val prefs = remember { UserPreferences(context) }
    val location = remember { LocationSubsystem.getInstance(context) }
    val formatService = remember { FormatService.getInstance(context) }

    TemperatureEstimationScreen(
        prefs = prefs,
        formatService = formatService,
        initialElevation = location.elevation.convertTo(prefs.baseDistanceUnits),
        sensorTemperature = { thermometer.temperature },
        hasThermometerReading = {
            thermometer.hasValidReading || thermometer.temperature != 0f
        },
        readThermometer = {
            readAll(
                listOf(thermometer),
                Duration.ofSeconds(10),
                forceStopOnCompletion = true
            )
        }
    )
}

@Composable
private fun TemperatureEstimationScreen(
    prefs: UserPreferences,
    formatService: FormatService,
    initialElevation: Distance,
    sensorTemperature: () -> Float,
    hasThermometerReading: () -> Boolean,
    readThermometer: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val temperatureUnits = prefs.temperatureUnits
    val temperatureUnitOptions = remember {
        if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit)
        } else {
            listOf(TemperatureUnits.Fahrenheit, TemperatureUnits.Celsius)
        }
    }
    val elevationUnits = remember { DistanceUtils.elevationDistanceUnits }
    val speedUnits = remember {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            listOf(
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond,
                WindSpeedUnit.MilesPerHour
            )
        } else {
            listOf(
                WindSpeedUnit.MilesPerHour,
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond
            )
        }
    }

    val roundedElevation = initialElevation.value.roundPlaces(
        Units.getDecimalPlaces(initialElevation.units)
    )
    var baseElevationText by remember { mutableStateOf(roundedElevation.toString()) }
    var baseElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var destElevationText by remember { mutableStateOf("") }
    var destElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var baseTemperatureText by remember { mutableStateOf("") }
    var baseTemperatureUnit by remember { mutableStateOf(temperatureUnits) }
    var humidityText by remember { mutableStateOf("") }
    var windSpeedText by remember { mutableStateOf("") }
    var windSpeedUnit by remember { mutableStateOf(speedUnits.first()) }
    var isLoading by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Picker?>(null) }

    fun setThermometerFieldFromSensor() {
        val sensorTemp = sensorTemperature()
        if (hasThermometerReading() && !sensorTemp.isNaN()) {
            val temp = Temperature.celsius(sensorTemp).convertTo(temperatureUnits)
            baseTemperatureText = temp.value.safeRoundToInt().toString()
            baseTemperatureUnit = temperatureUnits
        }
    }

    LaunchedEffect(Unit) {
        setThermometerFieldFromSensor()
    }

    val estimated = estimateTemperature(
        baseTemperatureText = baseTemperatureText,
        baseTemperatureUnit = baseTemperatureUnit,
        baseElevationText = baseElevationText,
        baseElevationUnit = baseElevationUnit,
        destElevationText = destElevationText,
        destElevationUnit = destElevationUnit,
        humidityText = humidityText,
        windSpeedText = windSpeedText,
        windSpeedUnit = windSpeedUnit,
        outputUnits = temperatureUnits
    )
    val title = estimated?.takeIf { !it.value.isNaN() }?.let {
        formatService.formatTemperature(it, 4, false)
    } ?: stringResource(R.string.dash)
    val humidity = humidityText.toFloatOrNull()
    val humidityError = humidityText.isNotEmpty() && (humidity == null || humidity !in 0f..100f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrailSenseSpacing.lg)
                .padding(top = TrailSenseSpacing.md, bottom = TrailSenseSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            // Hero result
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TrailSenseSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.estimated),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Inputs
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                DistanceInputRow(
                    value = baseElevationText,
                    onValueChange = { baseElevationText = it },
                    unit = baseElevationUnit,
                    unitName = formatService.getDistanceUnitName(baseElevationUnit, true),
                    label = stringResource(R.string.base_elevation),
                    onPickUnit = { picker = Picker.BaseElevation },
                    enabled = !isLoading
                )
                DistanceInputRow(
                    value = destElevationText,
                    onValueChange = { destElevationText = it },
                    unit = destElevationUnit,
                    unitName = formatService.getDistanceUnitName(destElevationUnit, true),
                    label = stringResource(R.string.destination_elevation),
                    onPickUnit = { picker = Picker.DestinationElevation },
                    enabled = !isLoading
                )
                UnitInputRow(
                    value = baseTemperatureText,
                    onValueChange = { baseTemperatureText = it },
                    unitName = formatService.getTemperatureUnitName(baseTemperatureUnit, true),
                    label = stringResource(R.string.current_temperature),
                    onPickUnit = { picker = Picker.Temperature },
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = humidityText,
                    onValueChange = { humidityText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.humidity)) },
                    isError = humidityError,
                    supportingText = {
                        if (humidityError) {
                            Text(stringResource(R.string.humidity_must_be_between_0_and_100))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                UnitInputRow(
                    value = windSpeedText,
                    onValueChange = { windSpeedText = it },
                    unitName = formatService.getSpeedUnitName(
                        windSpeedUnit.distance,
                        windSpeedUnit.time,
                        true
                    ),
                    label = stringResource(R.string.wind_speed),
                    onPickUnit = { picker = Picker.WindSpeed },
                    enabled = !isLoading
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        readThermometer()
                        setThermometerFieldFromSensor()
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                shape = MaterialTheme.shapes.large,
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        stringResource(R.string.autofill),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    when (picker) {
        Picker.BaseElevation -> UnitPickerDialog(
            title = stringResource(R.string.base_elevation),
            units = elevationUnits,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                baseElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.DestinationElevation -> UnitPickerDialog(
            title = stringResource(R.string.destination_elevation),
            units = elevationUnits,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                destElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.Temperature -> UnitPickerDialog(
            title = stringResource(R.string.current_temperature),
            units = temperatureUnitOptions,
            getName = { formatService.getTemperatureUnitName(it, true) },
            onSelect = {
                baseTemperatureUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.WindSpeed -> UnitPickerDialog(
            title = stringResource(R.string.wind_speed),
            units = speedUnits,
            getName = { formatService.getSpeedUnitName(it.distance, it.time, true) },
            onSelect = {
                windSpeedUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        null -> Unit
    }
}

@Composable
private fun DistanceInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    unit: DistanceUnits,
    unitName: String,
    label: String,
    onPickUnit: () -> Unit,
    enabled: Boolean
) {
    UnitInputRow(value, onValueChange, unitName, label, onPickUnit, enabled)
}

@Composable
private fun UnitInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    unitName: String,
    label: String,
    onPickUnit: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        FilledTonalButton(
            onClick = onPickUnit,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(unitName, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun <T> UnitPickerDialog(
    title: String,
    units: List<T>,
    getName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                units.forEach { unit ->
                    TextButton(
                        onClick = { onSelect(unit) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = getName(unit),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

private fun estimateTemperature(
    baseTemperatureText: String,
    baseTemperatureUnit: TemperatureUnits,
    baseElevationText: String,
    baseElevationUnit: DistanceUnits,
    destElevationText: String,
    destElevationUnit: DistanceUnits,
    humidityText: String,
    windSpeedText: String,
    windSpeedUnit: WindSpeedUnit,
    outputUnits: TemperatureUnits
): Temperature? {
    var temp = Temperature.from(
        baseTemperatureText.toFloatOrNull() ?: return null,
        baseTemperatureUnit
    ).convertTo(TemperatureUnits.Celsius)

    val destinationElevation = destElevationText.toFloatOrNull()?.let {
        Distance.from(it, destElevationUnit).meters()
    }
    val baseElevation = baseElevationText.toFloatOrNull()?.let {
        Distance.from(it, baseElevationUnit).meters()
    }
    val humidity = humidityText.toFloatOrNull()?.takeIf { it in 0f..100f }
    val windSpeed = windSpeedText.toFloatOrNull()?.let {
        Speed.from(it, windSpeedUnit.distance, windSpeedUnit.time)
    }

    if (destinationElevation != null) {
        if (baseElevation == null) {
            return null
        }
        temp = Meteorology.getTemperatureAtElevation(temp, baseElevation, destinationElevation)
    }

    if (humidity != null) {
        temp = Temperature.celsius(
            Meteorology.getHeatIndex(temp.celsius().value, humidity)
        ).convertTo(temp.units)
    }

    if (windSpeed != null) {
        temp = Meteorology.getWindChill(temp, windSpeed)
    }

    return temp.convertTo(outputUnits)
}

private enum class Picker {
    BaseElevation,
    DestinationElevation,
    Temperature,
    WindSpeed
}
