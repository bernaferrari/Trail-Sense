package com.kylecorry.andromeda.core.sensors

import com.kylecorry.andromeda.core.time.monotonicNanos
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

abstract class SharedSensor : Sensor {
    private val lock = SynchronizedObject()
    private val subscribers = mutableSetOf<SensorSubscriber>()
    override val quality: Quality get() = Quality.Unknown
    override var eventTimeElapsedNanos: Long = 0
        protected set
    override var timestamp: Instant = Instant.fromEpochSeconds(0)
        protected set

    override fun subscribe(subscriber: SensorSubscriber) = synchronized(lock) {
        val wasEmpty = subscribers.isEmpty()
        if (subscribers.add(subscriber) && wasEmpty) startImpl()
    }
    override fun unsubscribe(subscriber: SensorSubscriber) = synchronized(lock) {
        if (subscribers.remove(subscriber) && subscribers.isEmpty()) stopImpl()
    }
    override fun unsubscribeAll() = synchronized(lock) {
        if (subscribers.isNotEmpty()) {
            subscribers.clear()
            stopImpl()
        }
    }
    override fun start(subscriber: SensorSubscriber) = subscribe(subscriber)
    override fun stop(subscriber: SensorSubscriber?) {
        if (subscriber == null) unsubscribeAll() else unsubscribe(subscriber)
    }
    protected abstract fun startImpl()
    protected abstract fun stopImpl()
    protected fun notifyListeners() {
        val listeners = synchronized(lock) { subscribers.toList() }
        listeners.filterNot { it() }.forEach(::unsubscribe)
    }
    protected fun setEventTime(time: Instant) {
        val age = (Clock.System.now() - time).coerceAtLeast(Duration.ZERO)
        eventTimeElapsedNanos = monotonicNanos() - age.inWholeNanoseconds
        timestamp = time
    }
    protected fun setEventTimeFrom(sensor: Sensor) {
        eventTimeElapsedNanos = sensor.eventTimeElapsedNanos
        timestamp = sensor.timestamp
    }
    protected fun resetSessionMetadata() {
        eventTimeElapsedNanos = 0
        timestamp = Instant.fromEpochSeconds(0)
    }
}
