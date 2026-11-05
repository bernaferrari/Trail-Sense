@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.BackfillHistoricalTemperaturesCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

@Composable
fun ThermometerSettingsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    // The error banner lives on the activity; this is the only Activity-only API the page uses.
    val activity = LocalActivity.current as? MainActivity
    val viewModel = viewModel {
        ThermometerSettingsViewModel(
            prefs = UserPreferences(appContext),
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            sensorService = SensorService(appContext),
            weather = WeatherSubsystem.getInstance(appContext),
            keys = ThermometerPreferenceKeys(
                source = appContext.getString(R.string.pref_thermometer_source),
                smoothing = appContext.getString(R.string.pref_temperature_smoothing),
                calibration = listOf(
                    R.string.pref_min_calibrated_temp_c,
                    R.string.pref_max_calibrated_temp_c,
                    R.string.pref_min_uncalibrated_temp_c,
                    R.string.pref_max_uncalibrated_temp_c
                ).map { appContext.getString(it) }
            )
        )
    }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val cache = remember(appContext) { PreferencesSubsystem.getInstance(appContext).preferences }

    // Preference changes are followed while the page is started, as before.
    DisposableEffect(lifecycleOwner, viewModel) {
        val changes = cache.onChange.asLiveData()
        val observer = Observer<String> { viewModel.onPreferenceChanged(it) }
        changes.observe(lifecycleOwner, observer)
        onDispose { changes.removeObserver(observer) }
    }

    val thermometerSource = viewModel.thermometerSource
    LifecycleResumeEffect(viewModel, thermometerSource) {
        viewModel.startThermometer()
        if (activity != null) {
            displayLocationErrors(context, activity, { navigator.navigate(it) }, thermometerSource)
        }
        onPauseOrDispose {
            viewModel.stopThermometer()
            activity?.errorBanner?.dismiss(ErrorBannerReason.LocationNotSet)
        }
    }

    val sourceOptions = rememberSettingsOptions(
        R.array.thermometer_source_entries,
        R.array.thermometer_source_values
    )
    val source = viewModel.source
    val smoothing = viewModel.smoothing
    val calibration = viewModel.calibration
    val chartData = viewModel.chartData
    val temperature = viewModel.temperature?.let { formatService.formatTemperature(it) }
        ?: stringResource(R.string.dash)

    val sourceTitle = stringResource(R.string.source)
    val historyTitle = stringResource(R.string.history)
    val calibrationTitle = stringResource(R.string.calibrate)
    val aboutTitle = stringResource(R.string.pref_category_about)
    val noData = stringResource(R.string.no_data)
    val chartLegend = stringResource(R.string.calibration_chart_legend)
    val sourceFooter = stringResource(R.string.thermometer_source_explanation)
    val calibrationFooter = stringResource(R.string.thermometer_calibration_explanation)
    val thermometerIcon = rememberVectorPainter(TrailIcons.Thermometer)
    val sourceChoices = sourceOptions.map { TrailSenseChoice(it.value, it.label) }
    val entries = viewModel.calibrationEntries(calibration)

    TrailSensePage(
        title = stringResource(R.string.tool_thermometer_title),
        onBack = rememberGoBack(),
        backLabel = stringResource(R.string.back)
    ) {
        item("hero") {
            CalibrationHero(
                label = stringResource(R.string.temperature),
                value = temperature,
                icon = thermometerIcon,
                status = optionLabel(sourceOptions, source)
            )
        }

        trailSenseSection("history", title = historyTitle, footer = chartLegend) {
            row { position ->
                CalibrationHistoryChart(
                    data = chartData?.data,
                    raw = chartData?.raw.orEmpty(),
                    margin = 5f,
                    minRange = 10f,
                    emptyText = noData,
                    position = position
                )
            }
        }

        trailSenseSection("source", title = sourceTitle, footer = sourceFooter) {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    TrailSenseConnectedChoice(
                        options = sourceChoices,
                        selected = source,
                        label = sourceTitle,
                        onSelect = viewModel::selectSource
                    )
                }
            }
            row { position ->
                CalibrationSliderRow(
                    title = stringResource(R.string.smoothing),
                    summary = stringResource(R.string.smoothing_summary),
                    value = smoothing,
                    valueLabel = formatService.formatPercentage(smoothing / 10f),
                    range = 0..1000,
                    onValueChange = viewModel::changeSmoothing,
                    position = position
                )
            }
        }

        trailSenseSection("calibration", title = calibrationTitle, footer = calibrationFooter) {
            entries.forEach { entry ->
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(entry.title),
                        selected = DecimalFormatter.format(entry.value, 1),
                        onClick = {
                            pickTemperature(context, context.getString(entry.title), entry.value) { value ->
                                viewModel.setCalibration(entry, value)
                            }
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.reset_calibration),
                    onClick = viewModel::resetCalibration,
                    position = position
                )
            }
        }

        trailSenseSection("about", title = aboutTitle) {
            if (viewModel.backfillVisible) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.pref_backfill_temperatures_title),
                        onClick = {
                            Alerts.dialog(
                                context,
                                context.getString(R.string.pref_backfill_temperatures_title),
                                context.getString(R.string.pref_backfill_temperatures_description)
                            ) {
                                if (!it) {
                                    scope.launch {
                                        val loading =
                                            AlertLoadingIndicator(context, context.getString(R.string.updating))
                                        loading.show()
                                        BackfillHistoricalTemperaturesCommand.create(appContext).execute()
                                        loading.hide()
                                    }
                                }
                            }
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.tool_user_guide_title),
                    onClick = {
                        UserGuideUtils.showGuide(
                            context,
                            navigator,
                            R.raw.guide_tool_settings
                        )
                    },
                    position = position
                )
            }
        }
    }
}

private fun pickTemperature(
    context: Context,
    title: String,
    current: Float,
    onSelected: (Float) -> Unit
) {
    Pickers.number(
        context,
        title,
        null,
        current,
        allowDecimals = true,
        allowNegative = false,
        hint = context.getString(R.string.temperature)
    ) {
        if (it != null) {
            onSelected(it.toFloat())
        }
    }
}

private fun displayLocationErrors(
    context: Context,
    activity: MainActivity,
    navigate: (Route) -> Unit,
    source: ThermometerSource
) {
    if (source == ThermometerSource.Historic && isLocationUnset(context)) {
        val error = UserError(
            ErrorBannerReason.LocationNotSet,
            context.getString(R.string.location_not_set) + "\n" +
                    context.getString(R.string.for_historic_temperatures),
            TrailIcons.Satellite,
            context.getString(R.string.set)
        ) {
            activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
            navigate(Route.CalibrateGps)
        }
        activity.errorBanner.report(error)
    } else {
        activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
    }
}

private fun isLocationUnset(context: Context): Boolean {
    val diagnostic = GPSDiagnosticScanner()
    return diagnostic.quickScan(context)
        .any { it.id == GPSDiagnosticScanner.LOCATION_UNSET }
}

/** The preference keys the thermometer page reads and watches. */
internal data class ThermometerPreferenceKeys(
    val source: String,
    val smoothing: String,
    /** Calibration keys whose change means the calibrated thermometer must be rebuilt. */
    val calibration: List<String>
)

internal data class TemperatureChartData(
    val data: List<Reading<Float>>,
    val raw: List<Reading<Float>>
)

internal data class TemperatureCalibrationState(
    val minBatteryC: Float,
    val minActualC: Float,
    val maxBatteryC: Float,
    val maxActualC: Float,
    val minBatteryF: Float,
    val minActualF: Float,
    val maxBatteryF: Float,
    val maxActualF: Float
)

internal class TemperatureCalibrationEntry(
    @StringRes val title: Int,
    val value: Float,
    val onPicked: (Float) -> Unit
)

/**
 * The thermometer page's state: the live calibrated reading, the history chart, the source, smoothing and the four
 * calibration points. Plain Kotlin over the preferences, the sensor service and the weather subsystem.
 */
internal class ThermometerSettingsViewModel(
    private val prefs: UserPreferences,
    private val cache: IPreferences,
    private val sensorService: SensorService,
    private val weather: IWeatherSubsystem,
    private val keys: ThermometerPreferenceKeys
) : ViewModel() {

    private val temperatureUnits = prefs.temperatureUnits
    private var calibratedThermometer: IThermometer? = null
    private var isThermometerOn = false
    private var history: List<WeatherObservation> = emptyList()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = emptyList()
    private val runner = CoroutineQueueRunner()

    var temperature by mutableStateOf<Temperature?>(null)
        private set
    var chartData by mutableStateOf<TemperatureChartData?>(null)
        private set
    var source by mutableStateOf(cache.getString(keys.source) ?: "historic")
        private set
    var smoothing by mutableStateOf(cache.getInt(keys.smoothing) ?: 0)
        private set
    var calibration by mutableStateOf(readCalibration())
        private set
    var backfillVisible by mutableStateOf(prefs.thermometer.source == ThermometerSource.Historic)
        private set

    /** The source the errors are shown for; updated whenever the source changes. */
    var thermometerSource by mutableStateOf(prefs.thermometer.source)
        private set

    init {
        viewModelScope.launch {
            weather.weatherChanged.flow.collect {
                updateData()
            }
        }
        updateData()
    }

    fun selectSource(value: String) {
        source = value
        cache.putString(keys.source, value)
        onSourceChanged(if (value == "sensor") ThermometerSource.Sensor else ThermometerSource.Historic)
    }

    fun changeSmoothing(value: Int) {
        smoothing = value
        cache.putInt(keys.smoothing, value)
    }

    fun setCalibration(entry: TemperatureCalibrationEntry, value: Float) {
        entry.onPicked(value)
        calibration = readCalibration()
        reloadThermometer()
    }

    fun resetCalibration() {
        prefs.thermometer.resetThermometerCalibration()
        calibration = readCalibration()
        reloadThermometer()
    }

    fun onPreferenceChanged(key: String) {
        if (key in keys.calibration || key == keys.source) {
            reloadThermometer()
        }
        if (key == keys.source) {
            onSourceChanged(prefs.thermometer.source)
        }
    }

    fun startThermometer() {
        isThermometerOn = true
        reloadThermometer()
    }

    fun stopThermometer() {
        isThermometerOn = false
        calibratedThermometer?.stop(this::onThermometerUpdate)
    }

    /** The four calibration points in the user's units; each writes both the Celsius and Fahrenheit preference. */
    fun calibrationEntries(calibration: TemperatureCalibrationState): List<TemperatureCalibrationEntry> {
        return if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(
                TemperatureCalibrationEntry(R.string.min_temp_uncalibrated, calibration.minBatteryC) { value ->
                    prefs.thermometer.minBatteryTemperature = value
                    prefs.thermometer.minBatteryTemperatureF = celsiusToFahrenheit(value)
                },
                TemperatureCalibrationEntry(R.string.min_temp_calibrated, calibration.minActualC) { value ->
                    prefs.thermometer.minActualTemperature = value
                    prefs.thermometer.minActualTemperatureF = celsiusToFahrenheit(value)
                },
                TemperatureCalibrationEntry(R.string.max_temp_uncalibrated, calibration.maxBatteryC) { value ->
                    prefs.thermometer.maxBatteryTemperature = value
                    prefs.thermometer.maxBatteryTemperatureF = celsiusToFahrenheit(value)
                },
                TemperatureCalibrationEntry(R.string.max_temp_calibrated, calibration.maxActualC) { value ->
                    prefs.thermometer.maxActualTemperature = value
                    prefs.thermometer.maxActualTemperatureF = celsiusToFahrenheit(value)
                }
            )
        } else {
            listOf(
                TemperatureCalibrationEntry(R.string.min_temp_uncalibrated, calibration.minBatteryF) { value ->
                    prefs.thermometer.minBatteryTemperatureF = value
                    prefs.thermometer.minBatteryTemperature = fahrenheitToCelsius(value)
                },
                TemperatureCalibrationEntry(R.string.min_temp_calibrated, calibration.minActualF) { value ->
                    prefs.thermometer.minActualTemperatureF = value
                    prefs.thermometer.minActualTemperature = fahrenheitToCelsius(value)
                },
                TemperatureCalibrationEntry(R.string.max_temp_uncalibrated, calibration.maxBatteryF) { value ->
                    prefs.thermometer.maxBatteryTemperatureF = value
                    prefs.thermometer.maxBatteryTemperature = fahrenheitToCelsius(value)
                },
                TemperatureCalibrationEntry(R.string.max_temp_calibrated, calibration.maxActualF) { value ->
                    prefs.thermometer.maxActualTemperatureF = value
                    prefs.thermometer.maxActualTemperature = fahrenheitToCelsius(value)
                }
            )
        }
    }

    override fun onCleared() {
        calibratedThermometer?.stop(this::onThermometerUpdate)
    }

    private fun celsiusToFahrenheit(value: Float): Float {
        return Temperature.from(value, TemperatureUnits.Celsius).convertTo(TemperatureUnits.Fahrenheit).value
    }

    private fun fahrenheitToCelsius(value: Float): Float {
        return Temperature.from(value, TemperatureUnits.Fahrenheit).celsius().value
    }

    private fun updateData() {
        viewModelScope.launch {
            runner.replace {
                history = weather.getHistory()
                uncalibratedHistory = weather.getRawHistory()
                updateChart()
            }
        }
    }

    private fun updateChart() {
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            Reading(
                it.temperature.convertTo(temperatureUnits).value,
                it.time
            )
        }

        val displayRawReadings = uncalibratedHistory.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            Reading(
                Temperature.celsius(it.value.temperature).convertTo(temperatureUnits).value,
                it.time
            )
        }

        chartData = TemperatureChartData(displayReadings, displayRawReadings)
    }

    private fun onSourceChanged(newSource: ThermometerSource) {
        when (newSource) {
            ThermometerSource.Historic -> setSmoothing(0f)
            ThermometerSource.Sensor -> setSmoothing(0.2f)
        }
        backfillVisible = newSource == ThermometerSource.Historic
        thermometerSource = newSource
        resetCalibration()
    }

    private fun setSmoothing(value: Float) {
        prefs.thermometer.smoothing = value
        smoothing = (value * 1000).safeRoundToInt()
    }

    private fun reloadThermometer() {
        calibratedThermometer?.stop(this::onThermometerUpdate)
        calibratedThermometer = null
        if (!isThermometerOn) {
            return
        }
        calibratedThermometer = sensorService.getThermometer()
        calibratedThermometer?.start(this::onThermometerUpdate)
    }

    private fun onThermometerUpdate(): Boolean {
        val reading = calibratedThermometer?.temperature ?: return true
        temperature = Temperature.from(reading, TemperatureUnits.Celsius).convertTo(prefs.temperatureUnits)
        return true
    }

    private fun readCalibration(): TemperatureCalibrationState {
        return TemperatureCalibrationState(
            minBatteryC = prefs.thermometer.minBatteryTemperature,
            minActualC = prefs.thermometer.minActualTemperature,
            maxBatteryC = prefs.thermometer.maxBatteryTemperature,
            maxActualC = prefs.thermometer.maxActualTemperature,
            minBatteryF = prefs.thermometer.minBatteryTemperatureF,
            minActualF = prefs.thermometer.minActualTemperatureF,
            maxBatteryF = prefs.thermometer.maxBatteryTemperatureF,
            maxActualF = prefs.thermometer.maxActualTemperatureF
        )
    }
}
