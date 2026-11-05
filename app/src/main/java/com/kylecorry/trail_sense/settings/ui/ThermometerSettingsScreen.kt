@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.annotation.StringRes
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
import androidx.lifecycle.observe
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
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
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun ThermometerSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { ThermometerSettingsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content()
}

private class ThermometerSettingsController(private val host: TrailSenseDestinationHost) {

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private var calibratedThermometer: IThermometer? = null
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }

    private val weather by lazy { WeatherSubsystem.getInstance(host.requireContext()) }
    private var history: List<WeatherObservation> = emptyList()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = emptyList()

    private val runner = CoroutineQueueRunner()

    private var setTemperatureState: ((String) -> Unit)? = null
    private var setChartState: ((TemperatureChartData) -> Unit)? = null
    private var setCalibrationState: ((TemperatureCalibrationState) -> Unit)? = null
    private var setSmoothingState: ((Int) -> Unit)? = null
    private var setBackfillVisible: ((Boolean) -> Unit)? = null

    @Composable
    fun Content() {
        val sourceOptions = remember {
            options(R.array.thermometer_source_entries, R.array.thermometer_source_values)
        }

        var temperature by remember { mutableStateOf(getString(R.string.dash)) }
        var chartData by remember { mutableStateOf<TemperatureChartData?>(null) }
        var source by remember { mutableStateOf(getSourcePreference()) }
        var smoothing by remember { mutableStateOf(getInt(R.string.pref_temperature_smoothing, 0)) }
        var calibration by remember { mutableStateOf(getCalibrationState()) }
        var backfillVisible by remember {
            mutableStateOf(prefs.thermometer.source == ThermometerSource.Historic)
        }

        setTemperatureState = { temperature = it }
        setChartState = { chartData = it }
        setCalibrationState = { calibration = it }
        setSmoothingState = { smoothing = it }
        setBackfillVisible = { backfillVisible = it }

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
        val entries = calibrationEntries(calibration)

        TrailSensePage(
            title = stringResource(R.string.tool_thermometer_title),
            onBack = host::goBack,
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
                            onSelect = {
                                source = it
                                putString(R.string.pref_thermometer_source, it)
                                onSourceChanged(sourceFromPreference(it))
                            }
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
                        onValueChange = {
                            smoothing = it
                            putInt(R.string.pref_temperature_smoothing, it)
                        },
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
                                pickTemperature(getString(entry.title), entry.value) { value ->
                                    entry.onPicked(value)
                                    refreshCalibrationState()
                                    reloadThermometer()
                                }
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.reset_calibration),
                        onClick = ::resetCalibration,
                        position = position
                    )
                }
            }

            trailSenseSection("about", title = aboutTitle) {
                if (backfillVisible) {
                    row { position ->
                        SettingsActionRow(
                            label = stringResource(R.string.pref_backfill_temperatures_title),
                            onClick = ::backfillTemperatures,
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.tool_user_guide_title),
                        onClick = {
                            UserGuideUtils.showGuide(
                                host.requireContext(),
                                host.findNavigator(),
                                R.raw.guide_tool_settings
                            )
                        },
                        position = position
                    )
                }
            }
        }
    }

    /** The four calibration points in the user's units; each writes both the Celsius and Fahrenheit preference. */
    private fun calibrationEntries(calibration: TemperatureCalibrationState): List<CalibrationEntry> {
        return if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(
                CalibrationEntry(R.string.min_temp_uncalibrated, calibration.minBatteryC) { value ->
                    prefs.thermometer.minBatteryTemperature = value
                    prefs.thermometer.minBatteryTemperatureF = celsiusToFahrenheit(value)
                },
                CalibrationEntry(R.string.min_temp_calibrated, calibration.minActualC) { value ->
                    prefs.thermometer.minActualTemperature = value
                    prefs.thermometer.minActualTemperatureF = celsiusToFahrenheit(value)
                },
                CalibrationEntry(R.string.max_temp_uncalibrated, calibration.maxBatteryC) { value ->
                    prefs.thermometer.maxBatteryTemperature = value
                    prefs.thermometer.maxBatteryTemperatureF = celsiusToFahrenheit(value)
                },
                CalibrationEntry(R.string.max_temp_calibrated, calibration.maxActualC) { value ->
                    prefs.thermometer.maxActualTemperature = value
                    prefs.thermometer.maxActualTemperatureF = celsiusToFahrenheit(value)
                }
            )
        } else {
            listOf(
                CalibrationEntry(R.string.min_temp_uncalibrated, calibration.minBatteryF) { value ->
                    prefs.thermometer.minBatteryTemperatureF = value
                    prefs.thermometer.minBatteryTemperature = fahrenheitToCelsius(value)
                },
                CalibrationEntry(R.string.min_temp_calibrated, calibration.minActualF) { value ->
                    prefs.thermometer.minActualTemperatureF = value
                    prefs.thermometer.minActualTemperature = fahrenheitToCelsius(value)
                },
                CalibrationEntry(R.string.max_temp_uncalibrated, calibration.maxBatteryF) { value ->
                    prefs.thermometer.maxBatteryTemperatureF = value
                    prefs.thermometer.maxBatteryTemperature = fahrenheitToCelsius(value)
                },
                CalibrationEntry(R.string.max_temp_calibrated, calibration.maxActualF) { value ->
                    prefs.thermometer.maxActualTemperatureF = value
                    prefs.thermometer.maxActualTemperature = fahrenheitToCelsius(value)
                }
            )
        }
    }

    private fun celsiusToFahrenheit(value: Float): Float {
        return Temperature.from(value, TemperatureUnits.Celsius).convertTo(TemperatureUnits.Fahrenheit).value
    }

    private fun fahrenheitToCelsius(value: Float): Float {
        return Temperature.from(value, TemperatureUnits.Fahrenheit).celsius().value
    }

    private class CalibrationEntry(
        @StringRes val title: Int,
        val value: Float,
        val onPicked: (Float) -> Unit
    )

    fun onCreate() {
        val source = getString(R.string.pref_thermometer_source)
        val thermometerInvalidationKeys = listOf(
            R.string.pref_min_calibrated_temp_c,
            R.string.pref_max_calibrated_temp_c,
            R.string.pref_min_uncalibrated_temp_c,
            R.string.pref_max_uncalibrated_temp_c
        ).map { getString(it) } + listOf(source)
        cache.onChange.asLiveData().observe(host.lifecycleOwner) {
            if (thermometerInvalidationKeys.contains(it)) {
                reloadThermometer()
            }

            if (it == source) {
                onSourceChanged(prefs.thermometer.source)
            }
        }

        host.lifecycleScope.launch {
            weather.weatherChanged.flow.collect {
                updateData()
            }
        }
        updateData()
    }

    fun onResume() {
        reloadThermometer()
        displayErrors(prefs.thermometer.source)
    }

    fun onPause() {
        calibratedThermometer?.stop(this::onThermometerUpdate)
        hideLocationUnsetError()
    }

    fun onDestroy() {
        setTemperatureState = null
        setChartState = null
        setCalibrationState = null
        setSmoothingState = null
        setBackfillVisible = null
    }

    private fun updateData() {
        host.inBackground {
            runner.replace {
                history = weather.getHistory()
                uncalibratedHistory = weather.getRawHistory()
                onMain {
                    updateChart()
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

        setChartState?.invoke(TemperatureChartData(displayReadings, displayRawReadings))
    }

    private fun onSourceChanged(source: ThermometerSource) {
        when (source) {
            ThermometerSource.Historic -> setSmoothing(0f)
            ThermometerSource.Sensor -> setSmoothing(0.2f)
        }
        setBackfillVisible?.invoke(source == ThermometerSource.Historic)
        displayErrors(source)
        resetCalibration()
    }

    private fun resetCalibration() {
        prefs.thermometer.resetThermometerCalibration()
        refreshCalibrationState()
        reloadThermometer()
    }

    private fun setSmoothing(smoothing: Float) {
        prefs.thermometer.smoothing = smoothing
        setSmoothingState?.invoke((smoothing * 1000).safeRoundToInt())
    }

    private fun refreshCalibrationState() {
        setCalibrationState?.invoke(getCalibrationState())
    }

    private fun reloadThermometer() {
        calibratedThermometer?.stop(this::onThermometerUpdate)
        calibratedThermometer = sensorService.getThermometer()
        calibratedThermometer?.start(this::onThermometerUpdate)
    }

    private fun onThermometerUpdate(): Boolean {
        val temperature = calibratedThermometer?.temperature ?: return true
        setTemperatureState?.invoke(
            formatService.formatTemperature(
                Temperature.from(
                    temperature,
                    TemperatureUnits.Celsius
                ).convertTo(prefs.temperatureUnits)
            )
        )
        return true
    }

    private fun backfillTemperatures() {
        Alerts.dialog(
            host.requireContext(),
            getString(R.string.pref_backfill_temperatures_title),
            getString(R.string.pref_backfill_temperatures_description)
        ) {
            if (!it) {
                host.inBackground {
                    val loading =
                        AlertLoadingIndicator(host.requireContext(), getString(R.string.updating))
                    loading.show()
                    BackfillHistoricalTemperaturesCommand.create(host.requireContext()).execute()
                    loading.hide()
                }
            }
        }
    }

    private fun pickOption(
        title: String,
        options: List<ThermometerOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun pickTemperature(
        title: String,
        current: Float,
        onSelected: (Float) -> Unit
    ) {
        Pickers.number(
            host.requireContext(),
            title,
            null,
            current,
            allowDecimals = true,
            allowNegative = false,
            hint = getString(R.string.temperature)
        ) {
            if (it != null) {
                onSelected(it.toFloat())
            }
        }
    }

    private fun options(entriesId: Int, valuesId: Int): List<ThermometerOption> {
        val entries = host.getResources().getStringArray(entriesId)
        val values = host.getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            ThermometerOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun optionLabel(options: List<ThermometerOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getSourcePreference(): String {
        return cache.getString(getString(R.string.pref_thermometer_source)) ?: "historic"
    }

    private fun sourceFromPreference(value: String): ThermometerSource {
        return if (value == "sensor") ThermometerSource.Sensor else ThermometerSource.Historic
    }

    private fun getCalibrationState(): TemperatureCalibrationState {
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

    private fun getInt(key: Int, defaultValue: Int): Int {
        return cache.getInt(getString(key)) ?: defaultValue
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putInt(key: Int, value: Int) {
        cache.putInt(getString(key), value)
    }

    private fun displayErrors(source: ThermometerSource) {
        if (source == ThermometerSource.Historic && isLocationUnset()) {
            showLocationUnsetError()
        } else {
            hideLocationUnsetError()
        }
    }

    private fun hideLocationUnsetError() {
        val activity = host.mainActivity
        activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
    }

    private fun showLocationUnsetError() {
        val activity = host.mainActivity
        val appNavigator = host.findNavigator()
        val error = UserError(
            ErrorBannerReason.LocationNotSet,
            getString(R.string.location_not_set) + "\n" +
                    getString(R.string.for_historic_temperatures),
            TrailIcons.Satellite,
            getString(R.string.set)
        ) {
            activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
            appNavigator.navigate(Route.CalibrateGps)
        }
        activity.errorBanner.report(error)
    }

    private fun isLocationUnset(): Boolean {
        val diagnostic = GPSDiagnosticScanner()
        return diagnostic.quickScan(host.requireContext())
            .any { it.id == GPSDiagnosticScanner.LOCATION_UNSET }
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return host.getString(id, *args)
    }

    private data class TemperatureChartData(
        val data: List<Reading<Float>>,
        val raw: List<Reading<Float>>
    )

    private data class TemperatureCalibrationState(
        val minBatteryC: Float,
        val minActualC: Float,
        val maxBatteryC: Float,
        val maxActualC: Float,
        val minBatteryF: Float,
        val minActualF: Float,
        val maxBatteryF: Float,
        val maxActualF: Float
    )

    private data class ThermometerOption(
        val value: String,
        val label: String
    )
}
