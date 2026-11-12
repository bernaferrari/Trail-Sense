package com.kylecorry.trail_sense.shared.device

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.theme.isDynamicColorAvailable
import com.kylecorry.trail_sense.shared.sensors.SensorService

class AndroidDeviceProfile(context: Context) : DeviceProfile {

    private val context = context.applicationContext
    private val sensors by lazy { getAppService<SensorService>() }

    override val hasCompass: Boolean
        get() = sensors.hasCompass()

    override val hasGyroscope: Boolean
        get() = Sensors.hasSensor(context, Sensor.TYPE_GYROSCOPE)

    override val hasBarometer: Boolean
        get() = Sensors.hasBarometer(context)

    override val hasHygrometer: Boolean
        get() = Sensors.hasHygrometer(context)

    override val hasDynamicColors: Boolean
        get() = isDynamicColorAvailable()

    override val isMetricPreferred: Boolean
        get() = Resources.isMetricPreferred(context)

    override val uses24HourClock: Boolean
        get() = Resources.uses24HourClock(context)
}
