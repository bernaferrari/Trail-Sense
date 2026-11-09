package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.andromeda.sense.location.SatelliteStatusSensor
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.andromeda.sense.pedometer.Pedometer
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

/** Adapts GNSS status readings to the shared satellite sensor contract. */
class FlowSatelliteStatus(readings: Flow<Reading<List<Satellite>>>, scope: CoroutineScope) :
    FlowMeasurement<List<Satellite>>(readings, scope, emptyList()), SatelliteStatusSensor {
    override val satelliteDetails: List<Satellite>? get() = value.takeIf { hasValidReading }
    override val satellites: Int? get() = satelliteDetails?.count { it.usedInFix }
}

class FlowLightSensor(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), LightSensor {
    override val illuminance: Float get() = value
}

class FlowHygrometer(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), Hygrometer {
    override val humidity: Float get() = value
}

class FlowThermometer(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), Thermometer {
    override val temperature: Float get() = value
}

class FlowPedometer(readings: Flow<Reading<Int>>, scope: CoroutineScope) :
    FlowMeasurement<Int>(readings, scope, 0), Pedometer {
    override val steps: Int get() = value
}
