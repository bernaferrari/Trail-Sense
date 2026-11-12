@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.views.compose.AppHost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

@Composable
fun CalibrateBarometerToolScreen() {
    val viewModel = viewModel {
        CalibrateBarometerViewModel(
            prefs = getAppService<UserPreferences>(),
            weatherSubsystem = getAppService<IWeatherSubsystem>(),
            logger = WeatherLogger(30.seconds,
                500.milliseconds,
            )
        )
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.start()
        onPauseOrDispose {
            viewModel.stop()
        }
    }

    CalibrateBarometerContent(viewModel)
}

/**
 * The barometer calibration page's state: the latest calibrated pressure, the calibrated and raw history for the
 * chart, and the offset applied to the sensor. Plain Kotlin over the preferences and the weather subsystem.
 */
internal class CalibrateBarometerViewModel(
    val prefs: UserPreferences,
    private val weatherSubsystem: IWeatherSubsystem,
    private val logger: WeatherLogger
) : ViewModel() {

    val units: PressureUnits = prefs.pressureUnits
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
    }

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()
    private val runner = CoroutineQueueRunner()

    /** The latest calibrated pressure, or null before the first update. */
    var pressure by mutableStateOf<Pressure?>(null)
        private set
    var chartData by mutableStateOf<PressureChartData?>(null)
        private set
    var barometerOffset by mutableStateOf(prefs.weather.barometerOffset)
        private set

    /** The latest calibrated pressure, or zero when there is no history yet. */
    val currentPressure: Pressure
        get() = history.lastOrNull()?.pressure ?: Pressure.hpa(0f)

    init {
        viewModelScope.launch {
            weatherSubsystem.weatherChanged.flow.collect {
                updateBarometerData()
            }
        }
        updateBarometerData()
    }

    /** Samples the barometer and refreshes the reading while the page is shown. */
    fun start() {
        updateTimer.interval(200)
        logger.start()
    }

    fun stop() {
        updateTimer.stop()
        logger.stop()
    }

    override fun onCleared() {
        stop()
    }

    fun updateChart() {
        val displayReadings = history.filter {
            (Clock.System.now() - it.time) <= prefs.weather.pressureHistory
        }.map { it.pressureReading() }

        val useTemperature = prefs.weather.seaLevelFactorInTemp
        val useSeaLevel = prefs.weather.useSeaLevelPressure

        val displayRawReadings = uncalibratedHistory.filter {
            (Clock.System.now() - it.time) <= prefs.weather.pressureHistory
        }.map {
            if (useSeaLevel) {
                Reading(it.value.seaLevel(useTemperature), it.time)
            } else {
                Reading(Pressure.hpa(it.value.pressure), it.time)
            }
        }

        chartData = PressureChartData(
            displayReadings.map { Reading(it.value.convertTo(units).value, it.time) },
            displayRawReadings.map { Reading(it.value.convertTo(units).value, it.time) },
            margin = Pressure.hpa(1f).convertTo(units).value,
            minRange = Pressure.hpa(40f).convertTo(units).value
        )
    }

    fun update() {
        if (throttle.isThrottled()) {
            return
        }

        pressure = currentPressure
        barometerOffset = prefs.weather.barometerOffset
    }

    fun resetBarometerOffset() {
        prefs.weather.barometerOffset = 0f
        barometerOffset = 0f
        update()
    }

    /** Calibrates the barometer so it reads [actual] now. */
    fun calibrateTo(actual: Pressure) {
        val currentOffset = prefs.weather.barometerOffset
        val currentReading = currentPressure.value
        val rawReading = currentReading - currentOffset
        val newOffset = actual.hpa().value - rawReading

        prefs.weather.barometerOffset = newOffset
        barometerOffset = newOffset
        update()
    }

    private fun updateBarometerData() {
        viewModelScope.launch {
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
}

internal data class PressureChartData(
    val data: List<Reading<Float>>,
    val raw: List<Reading<Float>>,
    val margin: Float,
    val minRange: Float
)

@Composable
private fun CalibrateBarometerContent(viewModel: CalibrateBarometerViewModel) {
    val cache = remember { getAppService<Preferences>() }
    val formatService = remember { FormatService.getInstance() }
    val units = viewModel.units
    val dash = appString(Res.string.dash)
    val currentPressure = viewModel.pressure
    val pressure = currentPressure?.let {
        formatService.formatPressure(it.convertTo(units), Units.getDecimalPlaces(units))
    } ?: dash
    val pressureAvailable = currentPressure != null && currentPressure.value != 0f
    val chartData = viewModel.chartData
    val barometerOffset = viewModel.barometerOffset
    var useSeaLevelPressure by remember {
        mutableStateOf(cache.booleanOr(PreferenceKeys.useSeaLevelPressure, true))
    }
    var pressureSmoothing by remember {
        mutableStateOf(cache.getInt(PreferenceKeys.barometerPressureSmoothing) ?: 150)
    }
    var adjustForTemperature by remember {
        mutableStateOf(cache.booleanOr(PreferenceKeys.adjustForTemperature, false))
    }
    val formatOffset = { offset: Float ->
        formatService.formatPressure(
            Pressure.hpa(offset).convertTo(units),
            Units.getDecimalPlaces(units)
        )
    }

    val pressureTitle = appString(Res.string.pressure)
    val seaLevelTitle = appString(Res.string.pref_use_sea_level_pressure_title)
    val historyTitle = appString(Res.string.history)
    val calibrationTitle = appString(Res.string.calibrate)
    val noData = appString(Res.string.no_data)
    val chartLegend = appString(Res.string.calibration_chart_legend)
    val offsetFooter = appString(Res.string.barometer_offset_explanation)
    val barometerIcon = rememberVectorPainter(TrailIcons.Barometer)

    TrailSensePage(
        title = appString(Res.string.barometer),
        onBack = rememberGoBack(),
        backLabel = appString(Res.string.back)
    ) {
        item("hero") {
            CalibrationHero(
                label = if (useSeaLevelPressure) seaLevelTitle else pressureTitle,
                value = pressure,
                icon = barometerIcon,
                supportingText = if (barometerOffset != 0f) {
                    val sign = if (barometerOffset > 0f) "+" else ""
                    appString(Res.string.barometer_offset_applied, sign + formatOffset(barometerOffset))
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
                        cache.putBoolean(PreferenceKeys.useSeaLevelPressure, it)
                        viewModel.updateChart()
                    },
                    position = position
                )
            }
            if (useSeaLevelPressure) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_adjust_for_temperature_title),
                        summary = appString(Res.string.pref_adjust_for_temperature_summary),
                        checked = adjustForTemperature,
                        onCheckedChange = {
                            adjustForTemperature = it
                            cache.putBoolean(PreferenceKeys.adjustForTemperature, it)
                            viewModel.updateChart()
                        },
                        position = position
                    )
                }
            }
            row { position ->
                CalibrationSliderRow(
                    title = appString(Res.string.smoothing),
                    summary = appString(Res.string.smoothing_summary),
                    value = pressureSmoothing,
                    valueLabel = formatService.formatPercentage(pressureSmoothing / 10f),
                    range = 0..1000,
                    onValueChange = {
                        pressureSmoothing = it
                        cache.putInt(PreferenceKeys.barometerPressureSmoothing, it)
                    },
                    position = position
                )
            }
        }

        trailSenseSection("calibration", title = calibrationTitle, footer = offsetFooter) {
            if (pressureAvailable) {
                row { position ->
                    SettingsPickerRow(
                        label = appString(Res.string.barometer_offset),
                        selected = formatOffset(barometerOffset),
                        onClick = {
                            UnitPickers.pressure(
                                AppHost.current(),
                                getAppService<AppStrings>().get(Res.string.pressure),
                                getAppService<AppStrings>().get(Res.string.enter_the_current_pressure),
                                default = viewModel.currentPressure.convertTo(units)
                            ) {
                                if (it != null) {
                                    viewModel.calibrateTo(it)
                                }
                            }
                        },
                        position = position
                    )
                }
            }
            if (barometerOffset != 0f) {
                row { position ->
                    SettingsActionRow(
                        label = appString(Res.string.reset_calibration),
                        onClick = {
                            Alerts.dialog(AppHost.current(),
                                getAppService<AppStrings>().get(Res.string.reset_calibration_question)
                            ) { cancelled ->
                                if (!cancelled) {
                                    viewModel.resetBarometerOffset()
                                }
                            }
                        },
                        position = position
                    )
                }
            }
        }
    }
}
