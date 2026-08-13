@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.sensors.ui

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.kylecorry.andromeda.alerts.Alerts
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
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
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
fun SensorDetailsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { SensorDetailsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
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
            controller.onDestroy()
        }
    }

    SensorDetailsScreen(
        sensors = controller.sensorDetails,
        onShowSensorList = controller::showSensorList
    )
}

private class SensorDetailsController(private val host: TrailSenseDestinationHost) {

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }

    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val sensorDetailsMap = mutableMapOf<String, SensorDetails?>()
    var sensorDetails by mutableStateOf<List<SensorDetails>>(emptyList())
        private set
    private val handler = Handler(Looper.getMainLooper())
    private val sensorListener = { updateAllSensorDetails() }
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateAllSensorDetails()
            handler.postDelayed(this, 500)
        }
    }

    // Hardware sensors
    private val accelerometer by lazy { sensorService.getAccelerometer() }
    private val magnetometer by lazy { sensorService.getMagnetometer() }
    private val barometer by lazy { sensorService.getBarometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val battery by lazy { Battery(host.requireContext()) }
    private val gyroscope by lazy { sensorService.getGyroscope() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val cellSignal by lazy { sensorService.getCellSignal() }
    private val gps by lazy { sensorService.getGPS() }

    // Virtual sensors
    private val gravity by lazy { sensorService.getGravity() }
    private val compass by lazy { sensorService.getCompass() }
    private val altimeter by lazy { sensorService.getAltimeter() }

    // Cache
    private val cachedGPS by lazy { CachedGPS(host.requireContext(), 500) }
    private val cachedAltimeter by lazy { CachedAltimeter(host.requireContext(), 500) }

    private val unavailableText by lazy { host.getString(R.string.unavailable) }
    private val observedSensors: List<ISensor> by lazy {
        listOf(
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
    }

    fun onResume() {
        initializeUnavailableSensors()
        observedSensors.forEach { it.start(sensorListener) }
        updateAllSensorDetails()
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, 500)
    }

    fun onPause() {
        handler.removeCallbacks(updateRunnable)
        observedSensors.forEach { it.stop(sensorListener) }
    }

    fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        observedSensors.forEach { it.stop(sensorListener) }
    }

    private fun initializeUnavailableSensors() {
        if (!Sensors.hasCompass(host.requireContext())) {
            sensorDetailsMap["compass"] = SensorDetails(
                host.getString(R.string.pref_compass_sensor_title),
                "",
                host.getString(R.string.unavailable),
                CustomUiUtils.getQualityColor(Quality.Poor),
                R.drawable.ic_compass_icon
            )
        }
    }

    fun showSensorList() {
        val sensorManager = host.requireContext().getSystemService<SensorManager>()
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL)

        val description = sensors?.joinToString("\n\n") { sensor ->
            "${sensor.name}\n${sensor.stringType} (${sensor.type})"
        } ?: ""

        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.sensors),
            description,
            cancelText = null
        )
    }

    private fun updateAllSensorDetails(): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { updateAllSensorDetails() }
            return true
        }

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
        synchronized(this) {
            sensorDetails = sensorDetailsMap.toList().mapNotNull { it.second }.sortedBy { it.name }
        }
        return true
    }

    private fun updateGyro() {
        if (!Sensors.hasSensor(host.requireContext(), Sensor.TYPE_GYROSCOPE)) {
            return
        }
        val euler = gyroscope.orientation.toEuler()
        sensorDetailsMap["gyroscope"] = SensorDetails(
            host.getString(R.string.sensor_gyroscope),
            host.getString(
                R.string.roll_pitch_yaw,
                formatService.formatDegrees(euler.roll),
                formatService.formatDegrees(euler.pitch),
                formatService.formatDegrees(euler.yaw)
            ),
            formatService.formatQuality(gyroscope.quality),
            CustomUiUtils.getQualityColor(gyroscope.quality),
            R.drawable.ic_gyro
        )
    }

    private fun updateClock() {
        sensorDetailsMap["clock"] = SensorDetails(
            host.getString(R.string.tool_clock_title),
            formatService.formatTime(LocalTime.now()) + " " + ZonedDateTime.now().zone.getDisplayName(
                TextStyle.FULL,
                Locale.getDefault()
            ),
            formatService.formatQuality(Quality.Good),
            CustomUiUtils.getQualityColor(Quality.Good),
            R.drawable.ic_tool_clock
        )
    }

    private fun updateGPSCache() {
        sensorDetailsMap["gps_cache"] = SensorDetails(
            host.getString(R.string.gps_cache),
            formatService.formatLocation(cachedGPS.location),
            getGPSCacheStatus(),
            CustomUiUtils.getQualityColor(getGPSCacheQuality()),
            R.drawable.satellite
        )
    }

    private fun updateAltimeterCache() {
        val altitude = Distance.from(
            cachedAltimeter.altitude,
            DistanceUnits.Meters
        )
            .convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)

        sensorDetailsMap["altimeter_cache"] = SensorDetails(
            host.getString(R.string.altimeter_cache),
            formatService.formatDistance(altitude),
            getAltimeterCacheStatus(),
            CustomUiUtils.getQualityColor(getAltimeterCacheQuality()),
            R.drawable.ic_altitude
        )
    }

    private fun updateAltimeter() {
        val altitude = Distance.from(
            altimeter.altitude,
            DistanceUnits.Meters
        )
            .convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)
        sensorDetailsMap["altimeter"] = SensorDetails(
            host.getString(R.string.pref_altimeter_calibration_title),
            formatService.formatDistance(altitude),
            getAltimeterStatus(),
            getAltimeterColor(),
            R.drawable.ic_altitude
        )
    }

    private fun updateBattery() {
        val quality = when (battery.health) {
            BatteryHealth.Good -> Quality.Good
            BatteryHealth.Unknown -> Quality.Unknown
            else -> Quality.Poor
        }
        sensorDetailsMap["battery"] = SensorDetails(
            host.getString(R.string.tool_battery_title),
            formatService.formatPercentage(battery.percent),
            formatService.formatBatteryHealth(battery.health),
            CustomUiUtils.getQualityColor(quality),
            R.drawable.ic_tool_battery
        )
    }

    private fun updateBarometer() {
        if (barometer is MockBarometer) {
            return
        }
        val pressure =
            Pressure.from(barometer.pressure, PressureUnits.Hpa).convertTo(prefs.pressureUnits)
        sensorDetailsMap["barometer"] = SensorDetails(
            host.getString(R.string.barometer),
            formatService.formatPressure(
                pressure,
                Units.getDecimalPlaces(prefs.pressureUnits)
            ),
            formatService.formatQuality(barometer.quality),
            CustomUiUtils.getQualityColor(barometer.quality),
            R.drawable.ic_weather
        )
    }

    private fun updateGPS() {
        sensorDetailsMap["gps"] = SensorDetails(
            host.getString(R.string.gps),
            "${formatService.formatLocation(gps.location)}\n${gps.satellites ?: 0} ${host.getString(R.string.satellites)}",
            getGPSStatus(),
            getGPSColor(),
            R.drawable.satellite
        )
    }

    private fun updateCompass() {
        sensorDetailsMap["compass"] = if (compass is MockSensor) {
            SensorDetails(
                host.getString(R.string.pref_compass_sensor_title),
                "-",
                unavailableText,
                CustomUiUtils.getQualityColor(Quality.Unknown),
                R.drawable.ic_compass_icon
            )
        } else {
            SensorDetails(
                host.getString(R.string.pref_compass_sensor_title),
                formatService.formatDegrees(compass.rawBearing, replace360 = true),
                formatService.formatQuality(compass.quality),
                CustomUiUtils.getQualityColor(compass.quality),
                R.drawable.ic_compass_icon
            )
        }
    }

    private fun updateThermometer() {
        val temperature = Temperature.from(
            thermometer.temperature,
            TemperatureUnits.Celsius
        ).convertTo(prefs.temperatureUnits)
        sensorDetailsMap["thermometer"] = SensorDetails(
            host.getString(R.string.tool_thermometer_title),
            formatService.formatTemperature(temperature),
            formatService.formatQuality(thermometer.quality),
            CustomUiUtils.getQualityColor(thermometer.quality),
            R.drawable.thermometer
        )
    }

    private fun updateHygrometer() {
        if (hygrometer is MockHygrometer) {
            return
        }

        sensorDetailsMap["hygrometer"] = SensorDetails(
            host.getString(R.string.hygrometer),
            formatService.formatPercentage(hygrometer.humidity),
            formatService.formatQuality(hygrometer.quality),
            CustomUiUtils.getQualityColor(hygrometer.quality),
            R.drawable.ic_category_water
        )
    }

    private fun updateGravity() {
        sensorDetailsMap["gravity"] = SensorDetails(
            host.getString(R.string.gravity),
            formatService.formatAcceleration(gravity.acceleration.magnitude(), 2),
            formatService.formatQuality(gravity.quality),
            CustomUiUtils.getQualityColor(gravity.quality),
            R.drawable.ic_tool_cliff_height
        )
    }

    private fun updateAccelerometer() {
        sensorDetailsMap["accelerometer"] = SensorDetails(
            host.getString(R.string.accelerometer),
            formatService.formatAcceleration(accelerometer.acceleration.magnitude(), 2),
            formatService.formatQuality(accelerometer.quality),
            CustomUiUtils.getQualityColor(accelerometer.quality),
            R.drawable.ic_tool_cliff_height
        )
    }

    private fun updateCellSignal() {
        val signal = cellSignal.signals.maxByOrNull { it.strength }
        sensorDetailsMap["cell"] = SensorDetails(
            host.getString(R.string.cell_signal),
            "${
                formatService.formatCellNetwork(signal?.network)
            }\n${formatService.formatPercentage(signal?.strength ?: 0f)} (${
                formatService.formatDbm(
                    signal?.dbm ?: 0
                )
            })",
            formatService.formatQuality(signal?.quality ?: Quality.Unknown),
            CustomUiUtils.getQualityColor(signal?.quality ?: Quality.Unknown),
            CellSignalUtils.getCellQualityImage(signal?.quality)
        )
    }

    private fun updateMagnetometer() {
        sensorDetailsMap["magnetometer"] = SensorDetails(
            host.getString(R.string.magnetometer),
            formatService.formatMagneticField(magnetometer.magneticField.magnitude()),
            formatService.formatQuality(magnetometer.quality),
            CustomUiUtils.getQualityColor(magnetometer.quality),
            R.drawable.ic_tool_metal_detector
        )
    }

    @ColorInt
    private fun getAltimeterColor(): Int {
        if (altimeter is OverrideAltimeter) {
            return Resources.color(host.requireContext(), R.color.green)
        }

        if (altimeter is CachedAltimeter) {
            return Resources.color(host.requireContext(), R.color.red)
        }

        if (altimeter is BarometricAltimeter) {
            return CustomUiUtils.getQualityColor(altimeter.quality)
        }

        if (!altimeter.hasValidReading) {
            return Resources.color(host.requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(altimeter.quality)
    }

    private fun getAltimeterStatus(): String {
        if (altimeter is OverrideAltimeter) {
            return host.getString(R.string.gps_user)
        }

        if (altimeter is CachedGPS) {
            return host.getString(R.string.unavailable)
        }

        if (!altimeter.hasValidReading) {
            return host.getString(R.string.gps_searching)
        }

        return formatService.formatQuality(altimeter.quality)
    }

    @ColorInt
    private fun getGPSColor(): Int {
        if (gps is OverrideGPS) {
            return Resources.color(host.requireContext(), R.color.green)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(host.requireContext())) {
            return Resources.color(host.requireContext(), R.color.red)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return Resources.color(host.requireContext(), R.color.yellow)
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && (gps.satellites
                ?: 0) < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)
        ) {
            return Resources.color(host.requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getGPSCacheStatus(): String {
        return if (cachedGPS.location == Coordinate.zero) {
            host.getString(R.string.unavailable)
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
            host.getString(R.string.unavailable)
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
            return host.getString(R.string.gps_user)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(host.requireContext())) {
            return host.getString(R.string.unavailable)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return host.getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && (gps.satellites
                ?: 0) < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)
        ) {
            return host.getString(R.string.gps_searching)
        }

        return formatService.formatQuality(gps.quality)
    }

}

data class SensorDetails(
    val name: String,
    val description: String,
    val statusMessage: String,
    @ColorInt val statusColor: Int,
    @DrawableRes val statusIcon: Int
)

@Composable
private fun SensorDetailsScreen(
    sensors: List<SensorDetails>,
    onShowSensorList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("sensors_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = TrailSenseSpacing.screen,
                end = TrailSenseSpacing.screen,
                top = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TrailSenseSpacing.sm)
                        .testTag("sensor_details_title"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pref_sensor_details_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = onShowSensorList,
                        modifier = Modifier.testTag("sensor_details_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sensors),
                            contentDescription = stringResource(R.string.sensors),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.sensors),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = TrailSenseSpacing.xs,
                        bottom = TrailSenseSpacing.xs
                    )
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(vertical = TrailSenseSpacing.xs)) {
                        sensors.forEach { sensor ->
                            SensorDetailsRow(sensor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorDetailsRow(
    sensor: SensorDetails,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = TrailSenseSpacing.lg,
                vertical = TrailSenseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tonal circular icon chip
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(sensor.statusIcon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.width(TrailSenseSpacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sensor.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sensor.description.isNotBlank()) {
                Text(
                    text = sensor.description,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xxs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(TrailSenseSpacing.md))

        // Status indicator: colored dot + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .then(Modifier)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ComposeColor(sensor.statusColor),
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            Text(
                text = sensor.statusMessage,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
