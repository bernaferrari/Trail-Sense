package com.kylecorry.trail_sense.shared.sensors

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.kylecorry.luna.concurrency.IFlowable
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.SensorSubscriber
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Emits whenever the sensor reports a reading. The sensor runs while the flow is collected.
 */
val Sensor.flow: Flow<Unit>
    get() = callbackFlow {
        val subscriber: SensorSubscriber = {
            trySend(Unit)
            true
        }
        subscribe(subscriber)
        awaitClose { unsubscribe(subscriber) }
    }.buffer(1, BufferOverflow.DROP_OLDEST)

/**
 * Observes the sensor's readings as lifecycle-aware live data. The sensor runs while the live data is active.
 */
fun Sensor.asLiveData(): LiveData<Unit> = flow.asLiveData()

/**
 * Exposes the sensor's readings to luna APIs that consume an [IFlowable], such as FlowableTimer.
 */
fun Sensor.asFlowable(): IFlowable<Unit> {
    val sensor = this
    return object : IFlowable<Unit> {
        override val flow: Flow<Unit>
            get() = sensor.flow
    }
}
