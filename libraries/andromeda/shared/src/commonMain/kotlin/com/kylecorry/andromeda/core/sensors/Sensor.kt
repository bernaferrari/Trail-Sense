package com.kylecorry.andromeda.core.sensors

import kotlinx.atomicfu.atomic
import kotlin.time.Instant

public typealias SensorSubscriber = () -> Boolean

public interface Sensor {
    public val quality: Quality
    public val hasValidReading: Boolean
    public val eventTimeElapsedNanos: Long
    public val timestamp: Instant

    public fun subscribe(subscriber: SensorSubscriber)
    public fun unsubscribe(subscriber: SensorSubscriber)
    public fun unsubscribeAll()
    public fun start(subscriber: SensorSubscriber)
    public fun stop(subscriber: SensorSubscriber? = null)
}

public suspend fun Sensor.read(until: () -> Boolean = { true }) {
    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
        val completed = atomic(false)
        val listener: SensorSubscriber = {
            if (!continuation.isActive || completed.value) false else if (until()) {
                if (completed.compareAndSet(expect = false, update = true)) {
                    continuation.resumeWith(Result.success(Unit))
                }
                false
            } else true
        }
        continuation.invokeOnCancellation { unsubscribe(listener) }
        subscribe(listener)
        if (!continuation.isActive) unsubscribe(listener)
    }
}
