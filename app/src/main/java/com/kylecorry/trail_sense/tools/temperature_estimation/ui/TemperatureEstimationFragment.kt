package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useLifecycleEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.extensions.compose.useTimer
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.TextInputView
import com.kylecorry.trail_sense.shared.views.UnitInputView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

private enum class WindSpeedUnit(val distance: DistanceUnits, val time: TimeUnits) {
    KilometersPerHour(DistanceUnits.Kilometers, TimeUnits.Hours),
    MilesPerHour(DistanceUnits.Miles, TimeUnits.Hours),
    MetersPerSecond(DistanceUnits.Meters, TimeUnits.Seconds)
}

class TemperatureEstimationFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        TemperatureEstimationContent()
    }
}

@Composable
private fun Fragment.TemperatureEstimationContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = useService<UserPreferences>()
    val sensorService = useService<SensorService>()
    val thermometer = remember { sensorService.getThermometer() }
    val locationSubsystem = remember { LocationSubsystem.getInstance(context) }
    val formatService = remember { FormatService.getInstance(context) }

    val temperatureUnits = prefs.temperatureUnits
    val distanceUnitsPref = prefs.distanceUnits

    // View refs for calling methods like autofill/pause (required for custom views with side effects)
    var baseElevationView by remember { mutableStateOf<ElevationInputView?>(null) }
    var destElevationView by remember { mutableStateOf<ElevationInputView?>(null) }
    var baseTempView by remember { mutableStateOf<UnitInputView<TemperatureUnits>?>(null) }
    var windSpeedView by remember { mutableStateOf<UnitInputView<WindSpeedUnit>?>(null) }
    var humidityView by remember { mutableStateOf<TextInputView?>(null) }

    // Autofill loading state (Compose managed)
    var isAutofilling by remember { mutableStateOf(false) }

    // Result state, updated by timer polling the authoritative custom view state
    var resultText by remember { mutableStateOf("-") }

    // Setup units lists (same logic as old onViewCreated)
    val tempDisplayUnits = remember(temperatureUnits, formatService) {
        val units = if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit)
        } else {
            listOf(TemperatureUnits.Fahrenheit, TemperatureUnits.Celsius)
        }
        units.map {
            UnitInputView.DisplayUnit(
                it,
                formatService.getTemperatureUnitName(it, true),
                formatService.getTemperatureUnitName(it, false)
            )
        }
    }

    val windDisplayUnits = remember(distanceUnitsPref, formatService) {
        val units = if (distanceUnitsPref == UserPreferences.DistanceUnits.Meters) {
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
        units.map {
            val name = formatService.getSpeedUnitName(it.distance, it.time, true)
            UnitInputView.DisplayUnit(it, name, name)
        }
    }

    // Initial elevation (rounded like old code)
    val initialElevation = remember {
        val elev = locationSubsystem.elevation.convertTo(prefs.baseDistanceUnits)
        Distance.from(
            elev.value.roundPlaces(Units.getDecimalPlaces(elev.units)),
            elev.units
        )
    }

    // Timer to poll estimation (mirrors old 200ms intervalometer, using views as source of truth)
    val (startTimer, stopTimer) = useTimer(200L, autostart = false) {
        val est = computeEstimation(
            baseElevationView, destElevationView, baseTempView, humidityView, windSpeedView,
            formatService, temperatureUnits
        )
        onMain {
            resultText = if (est == null || est.value.isNaN()) {
                "-"
            } else {
                formatService.formatTemperature(est.convertTo(temperatureUnits))
            }
        }
    }

    useLifecycleEffect(Lifecycle.Event.ON_RESUME) {
        startTimer()
        // Initial sensor fill on resume like old behavior
        setInitialFromSensorIfNeeded(baseTempView, thermometer, temperatureUnits, formatService)
    }

    useLifecycleEffect(Lifecycle.Event.ON_PAUSE) {
        stopTimer()
        baseElevationView?.pause()
        destElevationView?.pause()
    }

    // One-time initial setup effect (like onViewCreated)
    LaunchedEffect(Unit) {
        // The AndroidViews handle their own initial config in factory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        // Result display (replaces old Toolbar title with large expressive text)
        Text(
            text = resultText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TrailSenseSpacing.md)
                .testTag("temperature_title")
        )

        TrailSenseCard {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)) {
                // Base elevation
                AndroidView(
                    factory = { ctx ->
                        ElevationInputView(ctx).apply {
                            hint = ctx.getString(R.string.base_elevation)
                            elevation = initialElevation
                            baseElevationView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_est_base_elevation")
                )

                // Dest elevation
                AndroidView(
                    factory = { ctx ->
                        ElevationInputView(ctx).apply {
                            hint = ctx.getString(R.string.destination_elevation)
                            destElevationView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_est_dest_elevation")
                )

                // Base temperature (UnitInputView)
                AndroidView(
                    factory = { ctx ->
                        UnitInputView<TemperatureUnits>(ctx).apply {
                            units = tempDisplayUnits
                            unit = temperatureUnits
                            hint = ctx.getString(R.string.current_temperature)
                            baseTempView = this
                        }
                    },
                    update = { view ->
                        // Keep unit in sync if prefs change (rare)
                        if (view.unit != temperatureUnits) view.unit = temperatureUnits
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_est_base_temperature")
                )

                // Humidity (TextInputView)
                AndroidView(
                    factory = { ctx ->
                        TextInputView(ctx, null).apply {
                            setHint(ctx.getString(R.string.humidity))
                            setInputType(
                                android.text.InputType.TYPE_CLASS_NUMBER or
                                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                            )
                            humidityView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_est_humidity")
                )

                // Wind speed
                AndroidView(
                    factory = { ctx ->
                        UnitInputView<WindSpeedUnit>(ctx).apply {
                            units = windDisplayUnits
                            unit = windDisplayUnits.firstOrNull()?.unit
                            hint = ctx.getString(R.string.wind_speed)
                            allowNegative = false
                            windSpeedView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_est_wind_speed")
                )

                // Autofill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrailSenseButton(
                        text = stringResource(R.string.autofill),
                        onClick = {
                            isAutofilling = true
                            baseTempView?.isEnabled = false
                            baseElevationView?.autofill()
                            // Background sensor read + fill (ported autofill logic using coroutine)
                            CoroutineScope(Dispatchers.Default).launch {
                                try {
                                    readAll(
                                        listOf(thermometer),
                                        Duration.ofSeconds(10),
                                        forceStopOnCompletion = true
                                    )
                                } catch (_: Exception) {
                                }
                                onMain {
                                    setInitialFromSensorIfNeeded(
                                        baseTempView,
                                        thermometer,
                                        temperatureUnits,
                                        formatService
                                    )
                                    isAutofilling = false
                                    baseTempView?.isEnabled = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("temp_est_autofill")
                    )

                    if (isAutofilling) {
                        Spacer(Modifier.height(TrailSenseSpacing.md))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = TrailSenseSpacing.md)
                                .size(24.dp)
                                .testTag("temp_est_loading")
                        )
                    }
                }
            }
        }
    }
}

// --- Helper functions (logic ported/adapted from original, now pure or view-interacting) ---

private fun computeEstimation(
    baseElevationView: ElevationInputView?,
    destElevationView: ElevationInputView?,
    baseTempView: UnitInputView<TemperatureUnits>?,
    humidityView: TextInputView?,
    windSpeedView: UnitInputView<WindSpeedUnit>?,
    formatService: FormatService,
    temperatureUnits: TemperatureUnits
): Temperature? {
    var temp = getBaseTemperature(baseTempView) ?: return null
    val destElevation = getDestElevation(destElevationView)
    val baseElevation = getBaseElevation(baseElevationView)
    val humidity = getHumidity(humidityView)
    val windSpeed = getWindSpeed(windSpeedView)

    if (destElevation != null) {
        if (baseElevation == null) return null
        temp = Meteorology.getTemperatureAtElevation(temp, baseElevation, destElevation)
    }

    if (humidity != null) {
        temp = Temperature.celsius(
            Meteorology.getHeatIndex(temp.celsius().value, humidity)
        ).convertTo(temp.units)
    }

    if (windSpeed != null) {
        temp = Meteorology.getWindChill(temp, windSpeed)
    }
    return temp
}

private fun getBaseTemperature(view: UnitInputView<TemperatureUnits>?): Temperature? {
    val amount = view?.amount?.toFloat() ?: return null
    val units = (view.unit as? TemperatureUnits) ?: return null
    return Temperature.from(amount, units).convertTo(TemperatureUnits.Celsius)
}

private fun getBaseElevation(view: ElevationInputView?): Distance? {
    return view?.elevation?.meters()
}

private fun getDestElevation(view: ElevationInputView?): Distance? {
    return view?.elevation?.meters()
}

private fun getHumidity(view: TextInputView?): Float? {
    return view?.text?.toString()?.toFloatCompat()?.takeIf { it in 0f..100f }
}

private fun getWindSpeed(view: UnitInputView<WindSpeedUnit>?): Speed? {
    val amount = view?.amount?.toFloat() ?: return null
    val unit = view.unit as? WindSpeedUnit ?: return null
    return Speed.from(amount, unit.distance, unit.time)
}

private fun setInitialFromSensorIfNeeded(
    baseTempView: UnitInputView<TemperatureUnits>?,
    thermometer: com.kylecorry.andromeda.sense.thermometer.IThermometer,
    temperatureUnits: TemperatureUnits,
    formatService: FormatService
) {
    val sensorTemperature = thermometer.temperature
    if ((thermometer.hasValidReading || sensorTemperature != 0f) && !sensorTemperature.isNaN()) {
        val temp = Temperature.celsius(sensorTemperature).convertTo(temperatureUnits)
        baseTempView?.amount = temp.value.safeRoundToInt()
        baseTempView?.unit = temperatureUnits
    }
}

@Preview(showBackground = true)
@Composable
private fun TemperatureEstimationContentPreview() {
    MaterialTheme {
        // Preview would require full Fragment context for hooks; showing structure only
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("42 °F", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            TrailSenseCard {
                Text("Base elevation, dest elevation, temp, humidity, wind inputs (AndroidView in real use)")
            }
            TrailSenseButton(text = "Autofill", onClick = {})
        }
    }
}
