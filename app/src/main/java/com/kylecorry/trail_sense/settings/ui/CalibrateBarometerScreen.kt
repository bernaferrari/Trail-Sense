@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CalibrateBarometerToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { CalibrateBarometerController(fragment) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onViewCreated()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
            controller.onDestroy()
        }
    }

    CalibrateBarometerContent(controller)
}

private class CalibrateBarometerController(private val fragment: TrailSenseComposeDestinationFragment) {

    val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }
    private val units: PressureUnits by lazy { prefs.pressureUnits }
    val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
    }

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(fragment.requireContext()) }
    private val runner = CoroutineQueueRunner()

    private val logger by lazy {
        WeatherLogger(
            fragment.requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
        )
    }

    private var setPressureState: ((PressureState) -> Unit)? = null
    private var setChartState: ((PressureChartData) -> Unit)? = null
    private var setBarometerOffset: ((Float) -> Unit)? = null

    fun onViewCreated() {
        fragment.observe(weatherSubsystem.weatherChanged) {
            updateBarometerData()
        }
        updateBarometerData()
    }

    fun onResume() {
        updateTimer.interval(200)
        logger.start()
    }

    fun onPause() {
        updateTimer.stop()
        logger.stop()
    }

    fun onDestroy() {
        setPressureState = null
        setChartState = null
        setBarometerOffset = null
    }

    fun setPressureState(updater: (PressureState) -> Unit) {
        setPressureState = updater
    }

    fun setChartState(updater: (PressureChartData) -> Unit) {
        setChartState = updater
    }

    fun setBarometerOffset(updater: (Float) -> Unit) {
        setBarometerOffset = updater
    }

    fun updateChart() {
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map { it.pressureReading() }

        val useTemperature = prefs.weather.seaLevelFactorInTemp
        val useSeaLevel = prefs.weather.useSeaLevelPressure

        val displayRawReadings = uncalibratedHistory.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            if (useSeaLevel) {
                Reading(it.value.seaLevel(useTemperature), it.time)
            } else {
                Reading(Pressure.hpa(it.value.pressure), it.time)
            }
        }

        setChartState?.invoke(
            PressureChartData(
                displayReadings.map { it.copy(value = it.value.convertTo(units)) },
                displayRawReadings.map { it.copy(value = it.value.convertTo(units)) }
            )
        )
    }

    fun update() {
        if (throttle.isThrottled()) {
            return
        }

        val currentPressure = getCurrentPressure()
        setPressureState?.invoke(
            PressureState(
                pressure = formatService.formatPressure(
                    currentPressure.convertTo(units),
                    Units.getDecimalPlaces(units)
                ),
                pressureAvailable = currentPressure.value != 0f
            )
        )
        setBarometerOffset?.invoke(prefs.weather.barometerOffset)
    }

    fun resetBarometerOffset(onReset: (Float) -> Unit) {
        Alerts.dialog(
            fragment.requireContext(),
            fragment.getString(R.string.reset_calibration_question)
        ) { cancelled ->
            if (!cancelled) {
                prefs.weather.barometerOffset = 0f
                onReset(0f)
                update()
            }
        }
    }

    fun pickBarometerOffset(onPicked: (Float) -> Unit) {
        CustomUiUtils.pickPressure(
            fragment.requireContext(),
            fragment.getString(R.string.pressure),
            fragment.getString(R.string.enter_the_current_pressure),
            default = getCurrentPressure().convertTo(units)
        ) {
            if (it == null) {
                return@pickPressure
            }

            val currentOffset = prefs.weather.barometerOffset
            val currentReading = getCurrentPressure().value
            val rawReading = currentReading - currentOffset
            val newOffset = it.hpa().value - rawReading

            prefs.weather.barometerOffset = newOffset
            onPicked(newOffset)
            update()
        }
    }

    fun formatOffset(offset: Float): String {
        return formatService.formatPressure(
            Pressure.hpa(offset).convertTo(units),
            Units.getDecimalPlaces(units)
        )
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(fragment.getString(key)) ?: defaultValue
    }

    fun getInt(key: Int, defaultValue: Int): Int {
        return cache.getInt(fragment.getString(key)) ?: defaultValue
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(fragment.getString(key), value)
    }

    fun putInt(key: Int, value: Int) {
        cache.putInt(fragment.getString(key), value)
    }

    private fun updateBarometerData() {
        fragment.inBackground {
            runner.replace {
                history = weatherSubsystem.getHistory()
                uncalibratedHistory = weatherSubsystem.getRawHistory(true)
                onMain {
                    updateChart()
                    update()
                }
            }
        }
    }

    private fun getCurrentPressure(): Pressure {
        return history.lastOrNull()?.pressure ?: Pressure.hpa(0f)
    }
}

private data class PressureState(
    val pressure: String,
    val pressureAvailable: Boolean
)

private data class PressureChartData(
    val data: List<Reading<Pressure>>,
    val raw: List<Reading<Pressure>>
)

@Composable
private fun CalibrateBarometerContent(controller: CalibrateBarometerController) {
        val prefs = controller.prefs
        val formatService = controller.formatService
        val dash = stringResource(R.string.dash)
        var pressure by remember { mutableStateOf(dash) }
        var pressureAvailable by remember { mutableStateOf(false) }
        var chartData by remember {
            mutableStateOf(PressureChartData(emptyList(), emptyList()))
        }
        var useSeaLevelPressure by remember {
            mutableStateOf(controller.getBoolean(R.string.pref_use_sea_level_pressure, true))
        }
        var pressureSmoothing by remember {
            mutableStateOf(controller.getInt(R.string.pref_barometer_pressure_smoothing, 150))
        }
        var adjustForTemperature by remember {
            mutableStateOf(controller.getBoolean(R.string.pref_adjust_for_temperature, false))
        }
        var barometerOffset by remember {
            mutableStateOf(prefs.weather.barometerOffset)
        }

        controller.setPressureState {
            pressure = it.pressure
            pressureAvailable = it.pressureAvailable
        }
        controller.setChartState {
            chartData = it
        }
        controller.setBarometerOffset {
            barometerOffset = it
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
                    TrailSenseSectionHeader(title = stringResource(R.string.barometer))
                }
                item("pressure") {
                    SettingsInfoRow(
                        label = stringResource(R.string.pressure),
                        summary = pressure,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("chart") {
                    PressureChartView(
                        data = chartData.data,
                        raw = chartData.raw,
                        modifier = Modifier
                            .padding(horizontal = TrailSenseSpacing.lg)
                            .padding(vertical = TrailSenseSpacing.sm)
                    )
                }
                item("sea_level") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_use_sea_level_pressure_title),
                        checked = useSeaLevelPressure,
                        onCheckedChange = {
                            useSeaLevelPressure = it
                            controller.putBoolean(R.string.pref_use_sea_level_pressure, it)
                            controller.updateChart()
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("smoothing") {
                    BarometerSmoothingRow(
                        smoothing = pressureSmoothing,
                        summary = formatService.formatPercentage(
                            pressureSmoothing / 10f
                        ),
                        onSmoothingChange = {
                            pressureSmoothing = it
                            controller.putInt(R.string.pref_barometer_pressure_smoothing, it)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("temperature_adjustment") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_adjust_for_temperature_title),
                        summary = stringResource(R.string.pref_adjust_for_temperature_summary),
                        checked = adjustForTemperature,
                        enabled = useSeaLevelPressure,
                        onCheckedChange = {
                            adjustForTemperature = it
                            controller.putBoolean(R.string.pref_adjust_for_temperature, it)
                            controller.updateChart()
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("offset") {
                    SettingsActionRow(
                        label = stringResource(R.string.barometer_offset),
                        summary = controller.formatOffset(barometerOffset),
                        enabled = pressureAvailable,
                        onClick = {
                            controller.pickBarometerOffset {
                                barometerOffset = it
                            }
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("reset") {
                    SettingsActionRow(
                        label = stringResource(R.string.reset_calibration),
                        onClick = {
                            controller.resetBarometerOffset {
                                barometerOffset = it
                            }
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("info") {
                    SettingsInfoRow(
                        label = "",
                        summary = stringResource(R.string.pref_barometer_info_summary),
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
}

@Composable
private fun PressureChartView(
    data: List<Reading<Pressure>>,
    raw: List<Reading<Pressure>>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val rawColor = Color(AppColor.Gray.color).copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height)),
        contentAlignment = Alignment.Center
    ) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startTime = data.firstOrNull()?.time ?: raw.firstOrNull()?.time ?: Instant.now()
            val points = data.map {
                Duration.between(startTime, it.time).toMillis() / 3_600_000f to it.value.value
            }
            val rawPoints = raw.map {
                Duration.between(startTime, it.time).toMillis() / 3_600_000f to it.value.value
            }
            val allPoints = points + rawPoints
            val units = data.firstOrNull()?.value?.units ?: PressureUnits.Hpa
            val margin = Pressure.hpa(1f).convertTo(units).value
            val minRange = Pressure.hpa(40f).convertTo(units).value
            val minX = allPoints.minOfOrNull { it.first } ?: 0f
            val maxX = allPoints.maxOfOrNull { it.first } ?: 1f
            val minYValue = allPoints.minOfOrNull { it.second } ?: 0f
            val maxYValue = allPoints.maxOfOrNull { it.second } ?: 1f
            val yMinimum = minYValue - margin
            val yMaximum = max(maxYValue + margin, yMinimum + minRange)
            val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f
            val yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f

            fun x(value: Float): Float {
                return ((value - minX) / xRange).coerceIn(0f, 1f) * size.width
            }

            fun y(value: Float): Float {
                return size.height - ((value - yMinimum) / yRange).coerceIn(0f, 1f) * size.height
            }

            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun drawSeries(points: List<Pair<Float, Float>>, color: Color, strokeWidth: Float) {
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x(start.first), y(start.second)),
                        end = androidx.compose.ui.geometry.Offset(x(end.first), y(end.second)),
                        strokeWidth = strokeWidth
                    )
                }
            }

            drawSeries(rawPoints, rawColor, 2.dp.toPx())
            drawSeries(points, primary, 2.dp.toPx())
        }
    }
}

@Composable
private fun BarometerSmoothingRow(
    smoothing: Int,
    summary: String,
    onSmoothingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.md)
    ) {
        Text(
            text = stringResource(R.string.smoothing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = smoothing.toFloat(),
            onValueChange = {
                onSmoothingChange(it.roundToInt().coerceIn(0, 1000))
            },
            valueRange = 0f..1000f,
            steps = 999
        )
    }
}
