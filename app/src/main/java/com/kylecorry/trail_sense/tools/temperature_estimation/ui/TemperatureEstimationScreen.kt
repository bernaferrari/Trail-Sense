@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import com.kylecorry.trail_sense.tools.weather.ui.SkyPill
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Thermometer
import com.kylecorry.trail_sense.shared.icons.trail.CategoryWater
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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

/**
 * The estimate leads; under it the weather where you are (the temperature field fills itself from the
 * thermometer with the button at its end), then the two elevations as their own group.
 */
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

    val weatherTitle = stringResource(R.string.weather)
    val elevationTitle = stringResource(R.string.elevation)
    val autofillLabel = stringResource(R.string.autofill)
    val humidityErrorText = stringResource(R.string.humidity_must_be_between_0_and_100)
    val labels = TemperatureLabels(
        baseElevation = stringResource(R.string.base_elevation),
        destinationElevation = stringResource(R.string.destination_elevation),
        currentTemperature = stringResource(R.string.current_temperature),
        humidity = stringResource(R.string.humidity),
        windSpeed = stringResource(R.string.wind_speed)
    )

    TrailSensePage(
        title = stringResource(R.string.tool_temperature_estimation_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back)
    ) {
        item("estimate") {
            val estimateCelsius = estimated?.takeIf { !it.value.isNaN() }?.celsius()?.value
            val baseInOutput = baseTemperatureText.toFloatOrNull()?.let {
                Temperature.from(it, baseTemperatureUnit).convertTo(temperatureUnits).value
            }
            val delta = estimated?.takeIf { !it.value.isNaN() }?.value?.let { e -> baseInOutput?.let { e - it } }
            TemperatureEstimateHero(
                title = title,
                celsius = estimateCelsius,
                delta = delta?.takeIf { kotlin.math.abs(it) >= 0.05f },
                unitSymbol = formatService.getTemperatureUnitName(temperatureUnits, true)
            )
        }

        trailSenseSection(key = "weather", title = weatherTitle) {
            row { position ->
                TrailSenseInputField(
                    value = baseTemperatureText,
                    onValueChange = { baseTemperatureText = it },
                    label = labels.currentTemperature,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getTemperatureUnitName(baseTemperatureUnit, true),
                    onUnitClick = { picker = Picker.Temperature },
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            readThermometer()
                                            setThermometerFieldFromSensor()
                                            isLoading = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = rememberVectorPainter(TrailIcons.Thermometer),
                                        contentDescription = autofillLabel
                                    )
                                }
                            }
                        }
                    }
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = humidityText,
                    onValueChange = { humidityText = it },
                    label = labels.humidity,
                    position = position,
                    isError = humidityError,
                    errorText = humidityErrorText,
                    unit = "%",
                    trailingIcon = {
                        Icon(
                            painter = rememberVectorPainter(TrailIcons.CategoryWater),
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md)
                        )
                    }
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = windSpeedText,
                    onValueChange = { windSpeedText = it },
                    label = labels.windSpeed,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getSpeedUnitName(windSpeedUnit.distance, windSpeedUnit.time, true),
                    onUnitClick = { picker = Picker.WindSpeed }
                )
            }
        }

        trailSenseSection(key = "elevation", title = elevationTitle) {
            row { position ->
                TrailSenseInputField(
                    value = baseElevationText,
                    onValueChange = { baseElevationText = it },
                    label = labels.baseElevation,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(baseElevationUnit, true),
                    onUnitClick = { picker = Picker.BaseElevation }
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = destElevationText,
                    onValueChange = { destElevationText = it },
                    label = labels.destinationElevation,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(destElevationUnit, true),
                    onUnitClick = { picker = Picker.DestinationElevation }
                )
            }
        }
    }

    when (picker) {
        Picker.BaseElevation -> UnitPickerDialog(
            title = labels.baseElevation,
            units = elevationUnits,
            selected = baseElevationUnit,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                baseElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.DestinationElevation -> UnitPickerDialog(
            title = labels.destinationElevation,
            units = elevationUnits,
            selected = destElevationUnit,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                destElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.Temperature -> UnitPickerDialog(
            title = labels.currentTemperature,
            units = temperatureUnitOptions,
            selected = baseTemperatureUnit,
            getName = { formatService.getTemperatureUnitName(it, true) },
            onSelect = {
                baseTemperatureUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.WindSpeed -> UnitPickerDialog(
            title = labels.windSpeed,
            units = speedUnits,
            selected = windSpeedUnit,
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

/**
 * The estimate as the screen's hero: a thermometer in a sun shape that warms from primary (cold) to tertiary (hot),
 * the value at display size, how far it is from the temperature where you are, and a thermometer bar from -30 to
 * 45 °C with the estimate marked on it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TemperatureEstimateHero(
    title: String,
    celsius: Float?,
    delta: Float?,
    unitSymbol: String
) {
    val tone = when {
        celsius == null -> TrailSenseTone.Secondary
        celsius <= 0f -> TrailSenseTone.Primary
        celsius >= 30f -> TrailSenseTone.Error
        celsius >= 18f -> TrailSenseTone.Tertiary
        else -> TrailSenseTone.Secondary
    }
    val colors = trailSenseToneColors(tone)
    val container by animateColorAsState(colors.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "estimateHero")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = colors.content
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkyFocalShape(
                    icon = TrailIcons.Thermometer,
                    shape = if (celsius != null && celsius <= 0f) {
                        MaterialShapes.Cookie12Sided.toShape()
                    } else {
                        MaterialShapes.Sunny.toShape()
                    },
                    size = 52.dp,
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Text(
                    text = stringResource(R.string.estimated),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (delta != null) {
                    SkyPill(
                        text = stringResource(
                            R.string.sky_temperature_change,
                            String.format(java.util.Locale.getDefault(), "%+.1f", delta) + " " + unitSymbol
                        ),
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
                }
            }
            // The estimate at display size with a verdict in words; before there is one, the instruction is the
            // headline (no dash), and the thermometer waits without a marker.
            if (celsius == null) {
                Text(
                    text = stringResource(R.string.sky_temperature_idle),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.sky_temperature_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.content.copy(alpha = 0.86f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 88.sp,
                        lineHeight = 92.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                    maxLines = 1,
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 88.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = stringResource(
                        when {
                            celsius < 0f -> R.string.sky_temperature_verdict_freezing
                            celsius < 10f -> R.string.sky_temperature_verdict_cold
                            celsius < 18f -> R.string.sky_temperature_verdict_cool
                            celsius < 30f -> R.string.sky_temperature_verdict_warm
                            else -> R.string.sky_temperature_verdict_hot
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.content.copy(alpha = 0.86f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ThermometerBar(celsius = celsius, markerColor = MaterialTheme.colorScheme.primary)
            // Word ticks under the bar, at freezing, mild, warm and hot.
            Box(Modifier.fillMaxWidth()) {
                listOf(
                    0.4f to R.string.sky_axis_freezing,
                    0.6f to R.string.sky_axis_mild,
                    0.8f to R.string.sky_axis_warm,
                    1f to R.string.sky_axis_hot
                ).forEach { (fraction, label) ->
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.content.copy(alpha = 0.72f),
                        modifier = Modifier.align(androidx.compose.ui.BiasAlignment(fraction * 2f - 1f, 0f))
                    )
                }
            }

        }
    }
}

/** A thermometer from -30 °C to 45 °C, cool to warm in the theme's colours, with the estimate as a ringed marker. */
@Composable
private fun ThermometerBar(celsius: Float?, markerColor: androidx.compose.ui.graphics.Color) {
    val cold = MaterialTheme.colorScheme.primary
    val mild = MaterialTheme.colorScheme.secondary
    val warm = MaterialTheme.colorScheme.tertiary
    val hot = MaterialTheme.colorScheme.error
    val ring = MaterialTheme.colorScheme.surfaceContainerLowest
    val fraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = ((celsius ?: 7.5f) + 30f) / 75f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "thermometer"
    )
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val barHeight = 18.dp.toPx()
        val top = (size.height - barHeight) / 2f
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(cold, mild, warm, hot)),
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f),
            alpha = if (celsius == null) 0.35f else 1f
        )
        // Freezing, as a small notch.
        val freezing = 30f / 75f * size.width
        drawLine(
            ring.copy(alpha = 0.9f),
            androidx.compose.ui.geometry.Offset(freezing, top + 2.dp.toPx()),
            androidx.compose.ui.geometry.Offset(freezing, top + barHeight - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        if (celsius != null) {
            val x = fraction.coerceIn(0f, 1f) * size.width
            val c = androidx.compose.ui.geometry.Offset(x.coerceIn(size.height / 2, size.width - size.height / 2), size.height / 2)
            drawCircle(ring, radius = size.height / 2f, center = c)
            drawCircle(markerColor, radius = size.height / 2f - 4.dp.toPx(), center = c)
        }
    }
}

private data class TemperatureLabels(
    val baseElevation: String,
    val destinationElevation: String,
    val currentTemperature: String,
    val humidity: String,
    val windSpeed: String
)

@Composable
private fun <T> UnitPickerDialog(
    title: String,
    units: List<T>,
    selected: T,
    getName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    TrailSenseOptionsDialog(
        title = title,
        options = units.map { getName(it) to it },
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss
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
