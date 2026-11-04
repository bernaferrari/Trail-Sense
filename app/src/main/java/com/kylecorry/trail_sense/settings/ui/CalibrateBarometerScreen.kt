@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun CalibrateBarometerToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CalibrateBarometerController(host) }
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

private class CalibrateBarometerController(private val host: TrailSenseDestinationHost) {

    val prefs by lazy { UserPreferences(host.requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    private val units: PressureUnits by lazy { prefs.pressureUnits }
    val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
    }

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(host.requireContext()) }
    private val runner = CoroutineQueueRunner()

    private val logger by lazy {
        WeatherLogger(
            host.requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
        )
    }

    private var setPressureState: ((PressureState) -> Unit)? = null
    private var setChartState: ((PressureChartData) -> Unit)? = null
    private var setBarometerOffset: ((Float) -> Unit)? = null

    fun onViewCreated() {
        host.lifecycleScope.launch {
            weatherSubsystem.weatherChanged.flow.collect {
                updateBarometerData()
            }
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
                displayReadings.map { Reading(it.value.convertTo(units).value, it.time) },
                displayRawReadings.map { Reading(it.value.convertTo(units).value, it.time) },
                margin = Pressure.hpa(1f).convertTo(units).value,
                minRange = Pressure.hpa(40f).convertTo(units).value
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
            host.requireContext(),
            host.getString(R.string.reset_calibration_question)
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
            host.requireContext(),
            host.getString(R.string.pressure),
            host.getString(R.string.enter_the_current_pressure),
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

    fun goBack() {
        host.goBack()
    }

    fun formatOffsetSummary(offset: Float): String {
        val sign = if (offset > 0f) "+" else ""
        return host.getString(R.string.barometer_offset_applied, sign + formatOffset(offset))
    }

    fun formatOffset(offset: Float): String {
        return formatService.formatPressure(
            Pressure.hpa(offset).convertTo(units),
            Units.getDecimalPlaces(units)
        )
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(host.getString(key)) ?: defaultValue
    }

    fun getInt(key: Int, defaultValue: Int): Int {
        return cache.getInt(host.getString(key)) ?: defaultValue
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(host.getString(key), value)
    }

    fun putInt(key: Int, value: Int) {
        cache.putInt(host.getString(key), value)
    }

    private fun updateBarometerData() {
        host.inBackground {
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
    val data: List<Reading<Float>>,
    val raw: List<Reading<Float>>,
    val margin: Float,
    val minRange: Float
)

@Composable
private fun CalibrateBarometerContent(controller: CalibrateBarometerController) {
    val prefs = controller.prefs
    val formatService = controller.formatService
    val dash = stringResource(R.string.dash)
    var pressure by remember { mutableStateOf(dash) }
    var pressureAvailable by remember { mutableStateOf(false) }
    var chartData by remember { mutableStateOf<PressureChartData?>(null) }
    var useSeaLevelPressure by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_use_sea_level_pressure, true))
    }
    var pressureSmoothing by remember {
        mutableStateOf(controller.getInt(R.string.pref_barometer_pressure_smoothing, 150))
    }
    var adjustForTemperature by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_adjust_for_temperature, false))
    }
    var barometerOffset by remember { mutableStateOf(prefs.weather.barometerOffset) }

    controller.setPressureState {
        pressure = it.pressure
        pressureAvailable = it.pressureAvailable
    }
    controller.setChartState { chartData = it }
    controller.setBarometerOffset { barometerOffset = it }

    val pressureTitle = stringResource(R.string.pressure)
    val seaLevelTitle = stringResource(R.string.pref_use_sea_level_pressure_title)
    val historyTitle = stringResource(R.string.history)
    val calibrationTitle = stringResource(R.string.calibrate)
    val noData = stringResource(R.string.no_data)
    val chartLegend = stringResource(R.string.calibration_chart_legend)
    val offsetFooter = stringResource(R.string.barometer_offset_explanation)
    val barometerIcon = painterResource(R.drawable.ic_barometer)

    TrailSensePage(
        title = stringResource(R.string.barometer),
        onBack = controller::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        item("hero") {
            CalibrationHero(
                label = if (useSeaLevelPressure) seaLevelTitle else pressureTitle,
                value = pressure,
                icon = barometerIcon,
                supportingText = if (barometerOffset != 0f) {
                    controller.formatOffsetSummary(barometerOffset)
                } else {
                    null
                }
            )
        }

        trailSenseSection("history", title = historyTitle, footer = chartLegend) {
            row { position ->
                CalibrationHistoryChart(
                    data = chartData?.data,
                    raw = chartData?.raw.orEmpty(),
                    margin = chartData?.margin ?: 0f,
                    minRange = chartData?.minRange ?: 0f,
                    emptyText = noData,
                    position = position
                )
            }
        }

        trailSenseSection("pressure", title = pressureTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = seaLevelTitle,
                    checked = useSeaLevelPressure,
                    onCheckedChange = {
                        useSeaLevelPressure = it
                        controller.putBoolean(R.string.pref_use_sea_level_pressure, it)
                        controller.updateChart()
                    },
                    position = position
                )
            }
            if (useSeaLevelPressure) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_adjust_for_temperature_title),
                        summary = stringResource(R.string.pref_adjust_for_temperature_summary),
                        checked = adjustForTemperature,
                        onCheckedChange = {
                            adjustForTemperature = it
                            controller.putBoolean(R.string.pref_adjust_for_temperature, it)
                            controller.updateChart()
                        },
                        position = position
                    )
                }
            }
            row { position ->
                CalibrationSliderRow(
                    title = stringResource(R.string.smoothing),
                    summary = stringResource(R.string.smoothing_summary),
                    value = pressureSmoothing,
                    valueLabel = formatService.formatPercentage(pressureSmoothing / 10f),
                    range = 0..1000,
                    onValueChange = {
                        pressureSmoothing = it
                        controller.putInt(R.string.pref_barometer_pressure_smoothing, it)
                    },
                    position = position
                )
            }
        }

        trailSenseSection("calibration", title = calibrationTitle, footer = offsetFooter) {
            if (pressureAvailable) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.barometer_offset),
                        selected = controller.formatOffset(barometerOffset),
                        onClick = { controller.pickBarometerOffset { barometerOffset = it } },
                        position = position
                    )
                }
            }
            if (barometerOffset != 0f) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.reset_calibration),
                        onClick = { controller.resetBarometerOffset { barometerOffset = it } },
                        position = position
                    )
                }
            }
        }
    }
}
