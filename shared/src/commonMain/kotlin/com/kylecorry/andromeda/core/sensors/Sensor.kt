package com.kylecorry.andromeda.core.sensors

import kotlinx.atomicfu.atomic
import kotlin.time.Instant

typealias SensorSubscriber = () -> Boolean

interface Sensor {
    val quality: Quality
    val hasValidReading: Boolean
    val eventTimeElapsedNanos: Long
    val timestamp: Instant

    fun subscribe(subscriber: SensorSubscriber)
    fun unsubscribe(subscriber: SensorSubscriber)
    fun unsubscribeAll()
    fun start(subscriber: SensorSubscriber)
    fun stop(subscriber: SensorSubscriber? = null)
}

suspend fun Sensor.read(until: () -> Boolean = { true }) {
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
