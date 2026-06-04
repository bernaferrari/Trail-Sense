package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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

class ThermometerSettingsFragment : Fragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private var calibratedThermometer: IThermometer? = null
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private var history: List<WeatherObservation> = emptyList()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = emptyList()

    private val runner = CoroutineQueueRunner()

    private var setTemperatureState: ((String) -> Unit)? = null
    private var setChartState: ((TemperatureChartData) -> Unit)? = null
    private var setCalibrationState: ((TemperatureCalibrationState) -> Unit)? = null
    private var setSmoothingState: ((Int) -> Unit)? = null
    private var setBackfillVisible: ((Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    val sourceOptions = remember {
                        options(R.array.thermometer_source_entries, R.array.thermometer_source_values)
                    }

                    var temperature by remember { mutableStateOf(getString(R.string.dash)) }
                    var chartData by remember {
                        mutableStateOf(TemperatureChartData(emptyList(), emptyList()))
                    }
                    var source by remember { mutableStateOf(getSourcePreference()) }
                    var smoothing by remember {
                        mutableStateOf(getInt(R.string.pref_temperature_smoothing, 0))
                    }
                    var calibration by remember {
                        mutableStateOf(getCalibrationState())
                    }
                    var backfillVisible by remember {
                        mutableStateOf(prefs.thermometer.source == ThermometerSource.Historic)
                    }

                    setTemperatureState = {
                        temperature = it
                    }
                    setChartState = {
                        chartData = it
                    }
                    setCalibrationState = {
                        calibration = it
                    }
                    setSmoothingState = {
                        smoothing = it
                    }
                    setBackfillVisible = {
                        backfillVisible = it
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
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.tool_thermometer_title)
                                )
                            }
                            item("temperature") {
                                SettingsInfoRow(
                                    label = stringResource(R.string.temperature),
                                    summary = temperature,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (chartData.data.isNotEmpty()) {
                                item("chart") {
                                    TemperatureChartView(
                                        data = chartData.data,
                                        raw = chartData.raw,
                                        modifier = Modifier
                                            .padding(horizontal = TrailSenseSpacing.lg)
                                            .padding(vertical = TrailSenseSpacing.sm)
                                    )
                                }
                            }
                            item("source") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.source),
                                    selected = optionLabel(sourceOptions, source),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.source),
                                            sourceOptions,
                                            source
                                        ) {
                                            source = it
                                            putString(R.string.pref_thermometer_source, it)
                                            onSourceChanged(sourceFromPreference(it))
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("smoothing") {
                                ThermometerSmoothingRow(
                                    smoothing = smoothing,
                                    summary = formatService.formatPercentage(smoothing / 10f),
                                    onSmoothingChange = {
                                        smoothing = it
                                        putInt(R.string.pref_temperature_smoothing, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            if (temperatureUnits == TemperatureUnits.Celsius) {
                                item("min_uncalibrated_c") {
                                    temperatureRow(
                                        R.string.min_temp_uncalibrated,
                                        calibration.minBatteryC
                                    ) {
                                        pickTemperature(
                                            getString(R.string.min_temp_uncalibrated),
                                            calibration.minBatteryC
                                        ) { value ->
                                            prefs.thermometer.minBatteryTemperature = value
                                            prefs.thermometer.minBatteryTemperatureF =
                                                Temperature.from(value, TemperatureUnits.Celsius)
                                                    .convertTo(TemperatureUnits.Fahrenheit).value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("min_calibrated_c") {
                                    temperatureRow(
                                        R.string.min_temp_calibrated,
                                        calibration.minActualC
                                    ) {
                                        pickTemperature(
                                            getString(R.string.min_temp_calibrated),
                                            calibration.minActualC
                                        ) { value ->
                                            prefs.thermometer.minActualTemperature = value
                                            prefs.thermometer.minActualTemperatureF =
                                                Temperature.from(value, TemperatureUnits.Celsius)
                                                    .convertTo(TemperatureUnits.Fahrenheit).value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("max_uncalibrated_c") {
                                    temperatureRow(
                                        R.string.max_temp_uncalibrated,
                                        calibration.maxBatteryC
                                    ) {
                                        pickTemperature(
                                            getString(R.string.max_temp_uncalibrated),
                                            calibration.maxBatteryC
                                        ) { value ->
                                            prefs.thermometer.maxBatteryTemperature = value
                                            prefs.thermometer.maxBatteryTemperatureF =
                                                Temperature.from(value, TemperatureUnits.Celsius)
                                                    .convertTo(TemperatureUnits.Fahrenheit).value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("max_calibrated_c") {
                                    temperatureRow(
                                        R.string.max_temp_calibrated,
                                        calibration.maxActualC
                                    ) {
                                        pickTemperature(
                                            getString(R.string.max_temp_calibrated),
                                            calibration.maxActualC
                                        ) { value ->
                                            prefs.thermometer.maxActualTemperature = value
                                            prefs.thermometer.maxActualTemperatureF =
                                                Temperature.from(value, TemperatureUnits.Celsius)
                                                    .convertTo(TemperatureUnits.Fahrenheit).value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                            } else {
                                item("min_uncalibrated_f") {
                                    temperatureRow(
                                        R.string.min_temp_uncalibrated,
                                        calibration.minBatteryF
                                    ) {
                                        pickTemperature(
                                            getString(R.string.min_temp_uncalibrated),
                                            calibration.minBatteryF
                                        ) { value ->
                                            prefs.thermometer.minBatteryTemperatureF = value
                                            prefs.thermometer.minBatteryTemperature =
                                                Temperature.from(value, TemperatureUnits.Fahrenheit)
                                                    .celsius().value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("min_calibrated_f") {
                                    temperatureRow(
                                        R.string.min_temp_calibrated,
                                        calibration.minActualF
                                    ) {
                                        pickTemperature(
                                            getString(R.string.min_temp_calibrated),
                                            calibration.minActualF
                                        ) { value ->
                                            prefs.thermometer.minActualTemperatureF = value
                                            prefs.thermometer.minActualTemperature =
                                                Temperature.from(value, TemperatureUnits.Fahrenheit)
                                                    .celsius().value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("max_uncalibrated_f") {
                                    temperatureRow(
                                        R.string.max_temp_uncalibrated,
                                        calibration.maxBatteryF
                                    ) {
                                        pickTemperature(
                                            getString(R.string.max_temp_uncalibrated),
                                            calibration.maxBatteryF
                                        ) { value ->
                                            prefs.thermometer.maxBatteryTemperatureF = value
                                            prefs.thermometer.maxBatteryTemperature =
                                                Temperature.from(value, TemperatureUnits.Fahrenheit)
                                                    .celsius().value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                                item("max_calibrated_f") {
                                    temperatureRow(
                                        R.string.max_temp_calibrated,
                                        calibration.maxActualF
                                    ) {
                                        pickTemperature(
                                            getString(R.string.max_temp_calibrated),
                                            calibration.maxActualF
                                        ) { value ->
                                            prefs.thermometer.maxActualTemperatureF = value
                                            prefs.thermometer.maxActualTemperature =
                                                Temperature.from(value, TemperatureUnits.Fahrenheit)
                                                    .celsius().value
                                            refreshCalibrationState()
                                            reloadThermometer()
                                        }
                                    }
                                }
                            }
                            item("reset") {
                                SettingsActionRow(
                                    label = stringResource(R.string.reset_calibration),
                                    onClick = ::resetCalibration,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (backfillVisible) {
                                item("backfill") {
                                    SettingsActionRow(
                                        label = stringResource(R.string.pref_backfill_temperatures_title),
                                        onClick = ::backfillTemperatures,
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                }
                            }
                            item("guide") {
                                SettingsActionRow(
                                    label = stringResource(R.string.tool_user_guide_title),
                                    onClick = {
                                        UserGuideUtils.showGuide(
                                            this@ThermometerSettingsFragment,
                                            R.raw.guide_tool_settings
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun androidx.compose.foundation.lazy.LazyItemScope.temperatureRow(
        title: Int,
        value: Float,
        onClick: () -> Unit
    ) {
        SettingsPickerRow(
            label = getString(title),
            selected = DecimalFormatter.format(value, 1),
            onClick = onClick,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val source = getString(R.string.pref_thermometer_source)
        val thermometerInvalidationKeys = listOf(
            R.string.pref_min_calibrated_temp_c,
            R.string.pref_max_calibrated_temp_c,
            R.string.pref_min_uncalibrated_temp_c,
            R.string.pref_max_uncalibrated_temp_c
        ).map { getString(it) } + listOf(source)
        cache.onChange.asLiveData().observe(viewLifecycleOwner) {
            if (thermometerInvalidationKeys.contains(it)) {
                reloadThermometer()
            }

            if (it == source) {
                onSourceChanged(prefs.thermometer.source)
            }
        }

        observe(weather.weatherChanged) {
            updateData()
        }
        updateData()
    }

    override fun onResume() {
        super.onResume()
        reloadThermometer()
        displayErrors(prefs.thermometer.source)
    }

    override fun onPause() {
        super.onPause()
        calibratedThermometer?.stop(this::onThermometerUpdate)
        hideLocationUnsetError()
    }

    override fun onDestroyView() {
        setTemperatureState = null
        setChartState = null
        setCalibrationState = null
        setSmoothingState = null
        setBackfillVisible = null
        super.onDestroyView()
    }

    private fun updateData() {
        inBackground {
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
            requireContext(),
            getString(R.string.pref_backfill_temperatures_title),
            getString(R.string.pref_backfill_temperatures_description)
        ) {
            if (!it) {
                inBackground {
                    val loading =
                        AlertLoadingIndicator(requireContext(), getString(R.string.updating))
                    loading.show()
                    BackfillHistoricalTemperaturesCommand.create(requireContext()).execute()
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

    private fun pickTemperature(
        title: String,
        current: Float,
        onSelected: (Float) -> Unit
    ) {
        Pickers.number(
            requireContext(),
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
        val entries = resources.getStringArray(entriesId)
        val values = resources.getStringArray(valuesId)
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
        val activity = requireMainActivity()
        activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
    }

    private fun showLocationUnsetError() {
        val activity = requireMainActivity()
        val navController = findNavController()
        val error = UserError(
            ErrorBannerReason.LocationNotSet,
            getString(R.string.location_not_set) + "\n" +
                    getString(R.string.for_historic_temperatures),
            R.drawable.satellite,
            getString(R.string.set)
        ) {
            activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
            navController.navigate(R.id.calibrateGPSFragment)
        }
        activity.errorBanner.report(error)
    }

    private fun isLocationUnset(): Boolean {
        val diagnostic = GPSDiagnosticScanner()
        return diagnostic.quickScan(requireContext())
            .any { it.id == GPSDiagnosticScanner.LOCATION_UNSET }
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

@Composable
private fun TemperatureChartView(
    data: List<Reading<Float>>,
    raw: List<Reading<Float>>,
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
                Duration.between(startTime, it.time).toMillis() / 3_600_000f to it.value
            }
            val rawPoints = raw.map {
                Duration.between(startTime, it.time).toMillis() / 3_600_000f to it.value
            }
            val allPoints = points + rawPoints
            val minX = allPoints.minOfOrNull { it.first } ?: 0f
            val maxX = allPoints.maxOfOrNull { it.first } ?: 1f
            val minYValue = allPoints.minOfOrNull { it.second } ?: 0f
            val maxYValue = allPoints.maxOfOrNull { it.second } ?: 1f
            val yMargin = 5f
            val yMinimum = minYValue - yMargin
            val yMaximum = max(maxYValue + yMargin, yMinimum + 10f)
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
private fun ThermometerSmoothingRow(
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
