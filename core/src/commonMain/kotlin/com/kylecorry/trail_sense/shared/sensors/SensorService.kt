package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.trail_sense.shared.sensors.altimeter.AltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Hands out the sensors the tools read, already chosen and calibrated from the user's settings. Each platform
 * registers its own implementation as an app service.
 */
interface SensorService {

    fun getGPS(
        frequency: Duration = DEFAULT_GPS_FREQUENCY,
        useCache: Boolean = false,
        tag: String? = null
    ): ISatelliteGPS

    fun hasLocationPermission(background: Boolean = false): Boolean

    fun getPedometer(): Pedometer

    fun getSpeedometer(gps: GpsSensor? = null, tag: String? = null): Speedometer

    fun getAltimeter(
        preferGPS: Boolean = false,
        gps: GpsSensor? = null,
        frequency: Duration = DEFAULT_GPS_FREQUENCY,
        tag: String? = null
    ): Altimeter

    fun hasCompass(): Boolean

    fun hasGyroscope(): Boolean

    fun getCompass(
        orientationSensor: OrientationSensor? = null,
        delay: Int = MOTION_SENSOR_DELAY
    ): CompassSensor

    fun getOrientation(delay: Int = MOTION_SENSOR_DELAY): OrientationSensor

    fun getDeviceOrientationSensor(): DeviceOrientation

    fun getBarometer(calibrated: Boolean = true): Barometer

    fun getThermometer(calibrated: Boolean = true): Thermometer

    fun getHygrometer(): Hygrometer

    fun getCellSignal(removeUnregisteredSignals: Boolean = true): CellSignalSensor

    fun getGravity(delay: Int = MOTION_SENSOR_DELAY): Accelerometer

    fun getMagnetometer(
        filtered: Boolean = false,
        delay: Int = MOTION_SENSOR_DELAY
    ): Magnetometer

    fun getGyroscope(delay: Int = MOTION_SENSOR_DELAY): OrientationSensor

    fun getAccelerometer(delay: Int = MOTION_SENSOR_DELAY): Accelerometer

    fun getLightSensor(): LightSensor

    /**
     * The GPS an altimeter reads from, looking through any wrappers, or null when it doesn't use one.
     */
    fun getGPSFromAltimeter(altimeter: Altimeter): GpsSensor? {
        return when {
            altimeter is GpsSensor -> altimeter
            altimeter is AltimeterWrapper -> getGPSFromAltimeter(altimeter.altimeter)
            else -> null
        }
    }

    companion object {
        /**
         * The update rate for motion sensors, matching Android's SensorManager.SENSOR_DELAY_UI.
         */
        const val MOTION_SENSOR_DELAY = 2
        val DEFAULT_GPS_FREQUENCY: Duration = 1.seconds
        val NAVIGATION_GPS_FREQUENCY: Duration = 200.milliseconds
        val SINGLE_FIX_GPS_FREQUENCY: Duration = 20.milliseconds

        // Long enough for the GPS to get a fix which satisfies the user's accuracy filter
        val GPS_READ_TIMEOUT: Duration = 30.seconds

        // Used where the user is waiting on a reading they can cancel
        val EXTENDED_GPS_READ_TIMEOUT: Duration = 1.minutes
    }
}
