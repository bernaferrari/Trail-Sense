package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.chart.Chart
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
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.charts.PressureChart
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class CalibrateBarometerFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val units: PressureUnits by lazy { prefs.pressureUnits }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
    }

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val runner = CoroutineQueueRunner()

    private val logger by lazy {
        WeatherLogger(
            requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
        )
    }

    private var setPressureState: ((PressureState) -> Unit)? = null
    private var setChartState: ((PressureChartData) -> Unit)? = null
    private var setBarometerOffset: ((Float) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var pressure by remember { mutableStateOf(getString(R.string.dash)) }
                    var pressureAvailable by remember { mutableStateOf(false) }
                    var chartData by remember {
                        mutableStateOf(PressureChartData(emptyList(), emptyList()))
                    }
                    var useSeaLevelPressure by remember {
                        mutableStateOf(getBoolean(R.string.pref_use_sea_level_pressure, true))
                    }
                    var pressureSmoothing by remember {
                        mutableStateOf(getInt(R.string.pref_barometer_pressure_smoothing, 150))
                    }
                    var adjustForTemperature by remember {
                        mutableStateOf(getBoolean(R.string.pref_adjust_for_temperature, false))
                    }
                    var barometerOffset by remember {
                        mutableStateOf(prefs.weather.barometerOffset)
                    }

                    setPressureState = {
                        pressure = it.pressure
                        pressureAvailable = it.pressureAvailable
                    }
                    setChartState = {
                        chartData = it
                    }
                    setBarometerOffset = {
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
                                        putBoolean(R.string.pref_use_sea_level_pressure, it)
                                        updateChart()
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
                                        putInt(R.string.pref_barometer_pressure_smoothing, it)
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
                                        putBoolean(R.string.pref_adjust_for_temperature, it)
                                        updateChart()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("offset") {
                                SettingsActionRow(
                                    label = stringResource(R.string.barometer_offset),
                                    summary = formatOffset(barometerOffset),
                                    enabled = pressureAvailable,
                                    onClick = {
                                        pickBarometerOffset {
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
                                        Alerts.dialog(
                                            requireContext(),
                                            getString(R.string.reset_calibration_question)
                                        ) { cancelled ->
                                            if (!cancelled) {
                                                prefs.weather.barometerOffset = 0f
                                                barometerOffset = 0f
                                                update()
                                            }
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
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(weatherSubsystem.weatherChanged) {
            updateBarometerData()
        }
        updateBarometerData()
    }

    override fun onResume() {
        super.onResume()
        updateTimer.interval(200)
        logger.start()
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
        logger.stop()
    }

    override fun onDestroyView() {
        setPressureState = null
        setChartState = null
        setBarometerOffset = null
        super.onDestroyView()
    }

    private fun updateBarometerData() {
        inBackground {
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

    private fun updateChart() {
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

    private fun update() {
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

    private fun pickBarometerOffset(onPicked: (Float) -> Unit) {
        CustomUiUtils.pickPressure(
            requireContext(),
            getString(R.string.pressure),
            getString(R.string.enter_the_current_pressure),
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

    private fun getCurrentPressure(): Pressure {
        return history.lastOrNull()?.pressure ?: Pressure.hpa(0f)
    }

    private fun formatOffset(offset: Float): String {
        return formatService.formatPressure(
            Pressure.hpa(offset).convertTo(units),
            Units.getDecimalPlaces(units)
        )
    }

    private fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(getString(key)) ?: defaultValue
    }

    private fun getInt(key: Int, defaultValue: Int): Int {
        return cache.getInt(getString(key)) ?: defaultValue
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private fun putInt(key: Int, value: Int) {
        cache.putInt(getString(key), value)
    }

    private data class PressureState(
        val pressure: String,
        val pressureAvailable: Boolean
    )

    private data class PressureChartData(
        val data: List<Reading<Pressure>>,
        val raw: List<Reading<Pressure>>
    )
}

@Composable
private fun PressureChartView(
    data: List<Reading<Pressure>>,
    raw: List<Reading<Pressure>>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            Chart(context).apply {
                tag = PressureChart(this)
            }
        },
        update = {
            (it.tag as PressureChart).plot(data, raw)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
    )
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
