package com.kylecorry.trail_sense.shared.sensors.thermometer

import android.content.Context
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.CoroutineIntervalSensor
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class HistoricThermometer(
    context: Context,
    frequency: Duration = 1.seconds
) :
    CoroutineIntervalSensor(frequency), Thermometer {

    private val location = getAppService<ILocationSubsystem>()
    private val weather = WeatherSubsystem.getInstance(context)

    override var temperature: Float = 0f
        private set(value) {
            field = value
            hasValidReading = true
        }

    override var hasValidReading: Boolean = false

    override suspend fun update() {
        // Purposely not calibrated because there's a wrapper for that
        temperature = weather.getTemperature(
            Clock.System.now(),
            location.location,
            location.elevation,
            false
        ).value.value
        super.update()
    }

}
