package com.kylecorry.andromeda.core.sensors

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
