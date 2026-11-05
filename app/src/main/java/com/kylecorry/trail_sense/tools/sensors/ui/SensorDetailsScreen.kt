@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.sensors.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.annotation.ColorInt
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.altitude.BarometricAltimeter
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.mock.MockBarometer
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.sensors.hygrometer.MockHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SensorDetailsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    // The sensors run in the ViewModel while the page is resumed; their rows are formatted here, in the
    // composition, every time a reading changes (or every half second).
    val viewModel = viewModel { SensorDetailsViewModel(SensorService(appContext), appContext) }
    val formatter = remember(context, viewModel) { SensorDetailsFormatter(context, viewModel) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {
            viewModel.onPause()
        }
    }

    val version = viewModel.version
    val reported = remember(formatter, version) { formatter.format() }

    SensorDetailsScreen(
        reported = reported,
        onShowSensorList = { showSensorList(context) }
    )
}

/**
 * The sensors the page reads, started while it is resumed. [version] changes whenever any of them reports and every
 * half second, so the composition formats the rows again.
 */
internal class SensorDetailsViewModel(
    sensorService: SensorService,
    // Battery and the cached sensors take a Context; this is the application context, never an Activity.
    appContext: Context
) : ViewModel() {

    // Hardware sensors
    val accelerometer = sensorService.getAccelerometer()
    val magnetometer = sensorService.getMagnetometer()
    val barometer = sensorService.getBarometer()
    val hygrometer = sensorService.getHygrometer()
    val battery = Battery(appContext)
    val gyroscope = sensorService.getGyroscope()
    val thermometer = sensorService.getThermometer()
    val cellSignal = sensorService.getCellSignal()
    val gps = sensorService.getGPS()

    // Virtual sensors
    val gravity = sensorService.getGravity()
    val compass = sensorService.getCompass()
    val altimeter = sensorService.getAltimeter()

    // Cache
    val cachedGPS = CachedGPS(appContext, 500)
    val cachedAltimeter = CachedAltimeter(appContext, 500)

    var version by mutableIntStateOf(0)
        private set

    private val observedSensors: List<ISensor> = listOf(
        cachedGPS,
        cachedAltimeter,
        gps,
        compass,
        barometer,
        altimeter,
        thermometer,
        hygrometer,
        gravity,
        accelerometer,
        cellSignal,
        magnetometer,
        battery,
        gyroscope
    )
    private val sensorListener: () -> Boolean = {
        viewModelScope.launch { version++ }
        true
    }
    private var ticker: Job? = null

    fun onResume() {
        observedSensors.forEach { it.start(sensorListener) }
        version++
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(500)
                version++
            }
        }
    }

    fun onPause() {
        ticker?.cancel()
        ticker = null
        observedSensors.forEach { it.stop(sensorListener) }
    }

    override fun onCleared() {
        onPause()
    }
}

/** Every sensor's row: its name, reading, status and status colour, from the readings in [sensors]. */
private class SensorDetailsFormatter(private val context: Context, private val sensors: SensorDetailsViewModel) {

    private val prefs = UserPreferences(context)
    private val formatService = FormatService.getInstance(context)
    private val sensorDetailsMap = mutableMapOf<String, SensorDetails?>()

    private val accelerometer get() = sensors.accelerometer
    private val magnetometer get() = sensors.magnetometer
    private val barometer get() = sensors.barometer
    private val hygrometer get() = sensors.hygrometer
    private val battery get() = sensors.battery
    private val gyroscope get() = sensors.gyroscope
    private val thermometer get() = sensors.thermometer
    private val cellSignal get() = sensors.cellSignal
    private val gps get() = sensors.gps
    private val gravity get() = sensors.gravity
    private val compass get() = sensors.compass
    private val altimeter get() = sensors.altimeter
    private val cachedGPS get() = sensors.cachedGPS
    private val cachedAltimeter get() = sensors.cachedAltimeter

    private val unavailableText = context.getString(R.string.unavailable)

    fun format(): List<SensorDetails> {
        updateGPSCache()
        updateAltimeterCache()
        updateGPS()
        updateCompass()
        updateBarometer()
        updateAltimeter()
        updateThermometer()
        updateHygrometer()
        updateGravity()
        updateAccelerometer()
        updateCellSignal()
        updateMagnetometer()
        updateBattery()
        updateGyro()
        updateClock()
        return sensorDetailsMap.toList()
            .mapNotNull { (key, details) -> details?.copy(category = sensorCategory(key)) }
            .sortedBy { it.name }
    }

    private fun updateGyro() {
        if (!Sensors.hasSensor(context, Sensor.TYPE_GYROSCOPE)) {
            return
        }
        val euler = gyroscope.orientation.toEuler()
        sensorDetailsMap["gyroscope"] = SensorDetails(
            context.getString(R.string.sensor_gyroscope),
            context.getString(
                R.string.roll_pitch_yaw,
                formatService.formatDegrees(euler.roll),
                formatService.formatDegrees(euler.pitch),
                formatService.formatDegrees(euler.yaw)
            ),
            formatService.formatQuality(gyroscope.quality),
            CustomUiUtils.getQualityColor(gyroscope.quality),
            TrailIcons.Gyro
        )
    }

    private fun updateClock() {
        sensorDetailsMap["clock"] = SensorDetails(
            context.getString(R.string.tool_clock_title),
            formatService.formatTime(LocalTime.now()) + " " + ZonedDateTime.now().zone.getDisplayName(
                TextStyle.FULL,
                Locale.getDefault()
            ),
            formatService.formatQuality(Quality.Good),
            CustomUiUtils.getQualityColor(Quality.Good),
            TrailIcons.ToolClock
        )
    }

    private fun updateGPSCache() {
        sensorDetailsMap["gps_cache"] = SensorDetails(
            context.getString(R.string.gps_cache),
            formatService.formatLocation(cachedGPS.location),
            getGPSCacheStatus(),
            CustomUiUtils.getQualityColor(getGPSCacheQuality()),
            TrailIcons.Satellite
        )
    }

    private fun updateAltimeterCache() {
        val altitude = Distance.from(
            cachedAltimeter.altitude,
            DistanceUnits.Meters
        )
            .convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)

        sensorDetailsMap["altimeter_cache"] = SensorDetails(
            context.getString(R.string.altimeter_cache),
            formatService.formatDistance(altitude),
            getAltimeterCacheStatus(),
            CustomUiUtils.getQualityColor(getAltimeterCacheQuality()),
            TrailIcons.Altitude
        )
    }

    private fun updateAltimeter() {
        val altitude = Distance.from(
            altimeter.altitude,
            DistanceUnits.Meters
        )
            .convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)
        sensorDetailsMap["altimeter"] = SensorDetails(
            context.getString(R.string.pref_altimeter_calibration_title),
            formatService.formatDistance(altitude),
            getAltimeterStatus(),
            getAltimeterColor(),
            TrailIcons.Altitude
        )
    }

    private fun updateBattery() {
        val quality = when (battery.health) {
            BatteryHealth.Good -> Quality.Good
            BatteryHealth.Unknown -> Quality.Unknown
            else -> Quality.Poor
        }
        sensorDetailsMap["battery"] = SensorDetails(
            context.getString(R.string.tool_battery_title),
            formatService.formatPercentage(battery.percent),
            formatService.formatBatteryHealth(battery.health),
            CustomUiUtils.getQualityColor(quality),
            TrailIcons.ToolBattery
        )
    }

    private fun updateBarometer() {
        if (barometer is MockBarometer) {
            return
        }
        val pressure =
            Pressure.from(barometer.pressure, PressureUnits.Hpa).convertTo(prefs.pressureUnits)
        sensorDetailsMap["barometer"] = SensorDetails(
            context.getString(R.string.barometer),
            formatService.formatPressure(
                pressure,
                Units.getDecimalPlaces(prefs.pressureUnits)
            ),
            formatService.formatQuality(barometer.quality),
            CustomUiUtils.getQualityColor(barometer.quality),
            TrailIcons.Weather
        )
    }

    private fun updateGPS() {
        sensorDetailsMap["gps"] = SensorDetails(
            context.getString(R.string.gps),
            // The status stays one word on the right. The satellite count that explains it is the row's quiet
            // supporting line; the coordinates wrap to two lines here and the GPS cache row already shows the fix, so
            // they only show when there is no count (a manual or unavailable GPS).
            gps.satellites?.let { context.resources.getQuantityString(R.plurals.sensor_gps_satellites, it, it) }
                ?: formatService.formatLocation(gps.location),
            getGPSStatus(),
            getGPSColor(),
            TrailIcons.Satellite
        )
    }

    private fun updateCompass() {
        sensorDetailsMap["compass"] = if (compass is MockSensor) {
            SensorDetails(
                context.getString(R.string.pref_compass_sensor_title),
                "-",
                unavailableText,
                CustomUiUtils.getQualityColor(Quality.Unknown),
                TrailIcons.CompassIcon
            )
        } else {
            SensorDetails(
                context.getString(R.string.pref_compass_sensor_title),
                formatService.formatDegrees(compass.rawBearing, replace360 = true),
                formatService.formatQuality(compass.quality),
                CustomUiUtils.getQualityColor(compass.quality),
                TrailIcons.CompassIcon
            )
        }
    }

    private fun updateThermometer() {
        val temperature = Temperature.from(
            thermometer.temperature,
            TemperatureUnits.Celsius
        ).convertTo(prefs.temperatureUnits)
        sensorDetailsMap["thermometer"] = SensorDetails(
            context.getString(R.string.tool_thermometer_title),
            formatService.formatTemperature(temperature),
            formatService.formatQuality(thermometer.quality),
            CustomUiUtils.getQualityColor(thermometer.quality),
            TrailIcons.Thermometer
        )
    }

    private fun updateHygrometer() {
        if (hygrometer is MockHygrometer) {
            return
        }

        sensorDetailsMap["hygrometer"] = SensorDetails(
            context.getString(R.string.hygrometer),
            formatService.formatPercentage(hygrometer.humidity),
            formatService.formatQuality(hygrometer.quality),
            CustomUiUtils.getQualityColor(hygrometer.quality),
            TrailIcons.CategoryWater
        )
    }

    private fun updateGravity() {
        sensorDetailsMap["gravity"] = SensorDetails(
            context.getString(R.string.gravity),
            formatService.formatAcceleration(gravity.acceleration.magnitude(), 2),
            formatService.formatQuality(gravity.quality),
            CustomUiUtils.getQualityColor(gravity.quality),
            TrailIcons.ToolCliffHeight
        )
    }

    private fun updateAccelerometer() {
        sensorDetailsMap["accelerometer"] = SensorDetails(
            context.getString(R.string.accelerometer),
            formatService.formatAcceleration(accelerometer.acceleration.magnitude(), 2),
            formatService.formatQuality(accelerometer.quality),
            CustomUiUtils.getQualityColor(accelerometer.quality),
            TrailIcons.ToolCliffHeight
        )
    }

    private fun updateCellSignal() {
        val signal = cellSignal.signals.maxByOrNull { it.strength }
        // A dBm of 0 is the radio not reporting one, not a perfect signal; leave it out rather than show it.
        val dbm = signal?.dbm?.takeIf { it != 0 }?.let { " (${formatService.formatDbm(it)})" } ?: ""
        sensorDetailsMap["cell"] = SensorDetails(
            context.getString(R.string.cell_signal),
            "${formatService.formatCellNetwork(signal?.network)} · " +
                "${formatService.formatPercentage(signal?.strength ?: 0f)}$dbm",
            formatService.formatQuality(signal?.quality ?: Quality.Unknown),
            CustomUiUtils.getQualityColor(signal?.quality ?: Quality.Unknown),
            getCellQualityIcon(signal?.quality)
        )
    }

    private fun updateMagnetometer() {
        sensorDetailsMap["magnetometer"] = SensorDetails(
            context.getString(R.string.magnetometer),
            formatService.formatMagneticField(magnetometer.magneticField.magnitude()),
            formatService.formatQuality(magnetometer.quality),
            CustomUiUtils.getQualityColor(magnetometer.quality),
            TrailIcons.ToolMetalDetector
        )
    }

    @ColorInt
    private fun getAltimeterColor(): Int {
        if (altimeter is OverrideAltimeter) {
            return Resources.color(context, R.color.green)
        }

        if (altimeter is CachedAltimeter) {
            return Resources.color(context, R.color.red)
        }

        if (altimeter is BarometricAltimeter) {
            return CustomUiUtils.getQualityColor(altimeter.quality)
        }

        if (!altimeter.hasValidReading) {
            return Resources.color(context, R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(altimeter.quality)
    }

    private fun getAltimeterStatus(): String {
        if (altimeter is OverrideAltimeter) {
            return context.getString(R.string.gps_user)
        }

        if (altimeter is CachedGPS) {
            return context.getString(R.string.unavailable)
        }

        if (!altimeter.hasValidReading) {
            return context.getString(R.string.gps_searching)
        }

        return formatService.formatQuality(altimeter.quality)
    }

    @ColorInt
    private fun getGPSColor(): Int {
        if (gps is OverrideGPS) {
            return Resources.color(context, R.color.green)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return Resources.color(context, R.color.red)
        }

        if (getAppService<LocationSubsystem>().isLocationStale) {
            return Resources.color(context, R.color.yellow)
        }

        if (!gps.hasValidReading || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return Resources.color(context, R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getGPSCacheStatus(): String {
        return if (cachedGPS.location == Coordinate.zero) {
            context.getString(R.string.unavailable)
        } else {
            formatService.formatQuality(Quality.Good)
        }
    }

    private fun getGPSCacheQuality(): Quality {
        return if (cachedGPS.location == Coordinate.zero) {
            Quality.Poor
        } else {
            Quality.Good
        }
    }

    private fun getAltimeterCacheStatus(): String {
        return if (cachedAltimeter.altitude == 0f) {
            context.getString(R.string.unavailable)
        } else {
            formatService.formatQuality(Quality.Good)
        }
    }

    private fun getAltimeterCacheQuality(): Quality {
        return if (cachedAltimeter.altitude == 0f) {
            Quality.Poor
        } else {
            Quality.Good
        }
    }

    private fun getGPSStatus(): String {
        if (gps is OverrideGPS) {
            return context.getString(R.string.gps_user)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return context.getString(R.string.unavailable)
        }

        if (getAppService<LocationSubsystem>().isLocationStale) {
            return context.getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return context.getString(R.string.gps_searching)
        }

        return formatService.formatQuality(gps.quality)
    }

}

private fun showSensorList(context: Context) {
    val sensorManager = context.getSystemService<SensorManager>()
    val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL)

    val description = sensors?.joinToString("\n\n") { sensor ->
        "${sensor.name}\n${sensor.stringType} (${sensor.type})"
    } ?: ""

    Alerts.dialog(
        context,
        context.getString(R.string.sensors),
        description,
        cancelText = null
    )
}

/** The cell signal bars for a quality; mirrors CellSignalUtils.getCellQualityImage. */
private fun getCellQualityIcon(quality: Quality?): ImageVector {
    return when (quality) {
        Quality.Poor -> TrailIcons.SignalCellular1
        Quality.Moderate -> TrailIcons.SignalCellular2
        Quality.Good -> TrailIcons.SignalCellular3
        else -> TrailIcons.SignalCellularOutline
    }
}

data class SensorDetails(
    val name: String,
    val description: String,
    val statusMessage: String,
    @ColorInt val statusColor: Int,
    val statusIcon: ImageVector,
    val category: SensorCategory = SensorCategory.Device
)

enum class SensorCategory { Location, Orientation, Environment, Device }

private fun sensorCategory(key: String): SensorCategory = when (key) {
    "gps", "gps_cache", "altimeter", "altimeter_cache", "cell" -> SensorCategory.Location
    "compass", "gyroscope", "gravity", "accelerometer", "magnetometer" -> SensorCategory.Orientation
    "barometer", "thermometer", "hygrometer" -> SensorCategory.Environment
    else -> SensorCategory.Device
}

/** The status colours the sensors report, read back as the family's tones. */
private fun sensorTone(@ColorInt color: Int): TrailSenseTone = when (color) {
    AppColor.Green.color -> TrailSenseTone.Good
    AppColor.Yellow.color -> TrailSenseTone.Caution
    AppColor.Red.color -> TrailSenseTone.Error
    else -> TrailSenseTone.Neutral
}

/**
 * Sensor details: how many sensors are good and which need a look, then the sensors grouped by what they measure,
 * each with its status in words. The raw Android sensor list sits behind the top bar action.
 */
@Composable
private fun SensorDetailsScreen(
    reported: List<SensorDetails>,
    onShowSensorList: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One question: are my sensors working? The count of good sensors and the verdict answer it.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    // A sensor that gives a reading but doesn't report its accuracy is working: "Unknown" next to a real value read
    // as a fault, so it says Working and sits with the good ones instead of under Needs attention.
    val unknownText = stringResource(R.string.unknown)
    val workingText = stringResource(R.string.sensor_status_working)
    val sensors = remember(reported, unknownText, workingText) {
        reported.map {
            if (it.statusMessage == unknownText && it.description.isNotBlank() && it.description != "-") {
                it.copy(statusMessage = workingText, statusColor = AppColor.Green.color)
            } else {
                it
            }
        }
    }
    val tones = sensors.map { sensorTone(it.statusColor) }
    val good = tones.count { it == TrailSenseTone.Good }
    val moderate = tones.count { it == TrailSenseTone.Caution }
    val poor = tones.count { it == TrailSenseTone.Error }
    TrailSensePage(
        // The same name as its tile in Tools.
        title = stringResource(R.string.sensors),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("sensor_details_title"),
        actions = {
            IconButton(
                onClick = onShowSensorList,
                modifier = Modifier.testTag("sensor_details_button")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Sensors),
                    contentDescription = stringResource(R.string.sensors)
                )
            }
        }
    ) {
        // Readings arrive a moment after the screen opens; until then the page stays quiet instead of empty.
        if (sensors.isNotEmpty()) {
            item(key = "health") {
                SensorHealthHero(good = good, moderate = moderate, poor = poor, total = sensors.size)
            }
            item(key = "sensors") {
                Column(
                    modifier = Modifier.testTag("sensors_list"),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    // The sensors that need a look lead, in their own group, so the verdict above points at them;
                    // the good ones follow, grouped by what they measure.
                    val attention = sensors.filterIndexed { index, _ -> tones[index] != TrailSenseTone.Good }
                    if (attention.isNotEmpty()) {
                        TrailSenseRowGroup(title = stringResource(R.string.sky_sensors_needs_attention)) {
                            attention.forEach { sensor ->
                                row { position -> SensorDetailsRow(sensor, position) }
                            }
                        }
                    }
                    SensorCategory.entries.forEach { category ->
                        val inCategory = sensors.filter { it.category == category && it !in attention }
                        if (inCategory.isNotEmpty()) {
                            TrailSenseRowGroup(
                                title = stringResource(
                                    when (category) {
                                        SensorCategory.Location -> R.string.location
                                        SensorCategory.Orientation -> R.string.sky_sensors_orientation
                                        SensorCategory.Environment -> R.string.sky_sensors_environment
                                        SensorCategory.Device -> R.string.sky_sensors_device
                                    }
                                )
                            ) {
                                inCategory.forEach { sensor ->
                                    row { position -> SensorDetailsRow(sensor, position) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The answer as WiFi Weakspot's signal card, like Diagnostics': a quiet surface where only the cookie wears the
 * verdict's colour, holding a check or a warning. The Needs attention group right below names the rest, so the line only speaks when all is well.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SensorHealthHero(good: Int, moderate: Int, poor: Int, total: Int) {
    val tone = when {
        poor > 0 -> TrailSenseTone.Error
        moderate > 0 -> TrailSenseTone.Caution
        else -> TrailSenseTone.Good
    }
    val colors = trailSenseToneColors(tone)
    TrailSenseSpotlight(
        title = stringResource(R.string.sky_sensors_good, good, total),
        supportingText = if (poor + moderate == 0) stringResource(R.string.sky_sensors_all_good) else null,
        tone = TrailSenseTone.Neutral,
        titleStyle = MaterialTheme.typography.headlineSmall,
        badge = {
            TrailSenseShapeBadge(
                icon = if (tone == TrailSenseTone.Good) {
                    androidx.compose.material.icons.Icons.Default.CheckCircle
                } else {
                    androidx.compose.material.icons.Icons.Default.WarningAmber
                },
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        }
    )
}

@Composable
private fun SensorDetailsRow(
    sensor: SensorDetails,
    position: TrailSenseGroupPosition
) {
    TrailSenseListItem(
        title = sensor.name,
        subtitle = sensor.description.takeIf { it.isNotBlank() },
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(sensor.statusIcon),
        position = position,
        trailing = { SensorStatus(sensor) }
    )
}

/**
 * The status in words. A good sensor stays quiet; one that needs a look gets a small dot and the attention role
 * (tertiary), and only a poor one turns to error.
 */
@Composable
private fun SensorStatus(sensor: SensorDetails) {
    val tone = sensorTone(sensor.statusColor)
    val unknown = sensor.statusMessage == stringResource(R.string.unknown)
    val color = when {
        tone == TrailSenseTone.Good -> MaterialTheme.colorScheme.onSurfaceVariant
        tone == TrailSenseTone.Error && !unknown -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    Spacer(Modifier.width(TrailSenseSpacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (tone != TrailSenseTone.Good) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(TrailSenseSpacing.xs))
        }
        Text(
            text = sensor.statusMessage,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            // One status word per row, never two lines jammed into the cell.
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
    }
}
