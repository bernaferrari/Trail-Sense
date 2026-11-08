package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.light.ILightSensor
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class FlowMeasurement<T>(
    private val readings: Flow<Reading<T>>,
    private val scope: CoroutineScope,
    initialValue: T,
) : SharedSensor() {
    protected var value: T = initialValue
        private set
    final override var hasValidReading: Boolean = false
        private set
    private var collection: Job? = null

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                value = reading.value
                hasValidReading = true
                setEventTime(reading.time)
                notifyListeners()
            }
        }
        collection?.start()
    }

    override fun stopImpl() {
        collection?.cancel()
        collection = null
    }
}

/** Adapts altitude readings in meters to the shared altimeter contract. */
class FlowAltimeter(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), Altimeter {
    override val altitude: Float get() = value
}

class FlowLightSensor(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), ILightSensor {
    override val illuminance: Float get() = value
}

class FlowHygrometer(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), IHygrometer {
    override val humidity: Float get() = value
}

class FlowThermometer(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), Thermometer {
    override val temperature: Float get() = value
}

class FlowPedometer(readings: Flow<Reading<Int>>, scope: CoroutineScope) :
    FlowMeasurement<Int>(readings, scope, 0), IPedometer {
    override val steps: Int get() = value
}
