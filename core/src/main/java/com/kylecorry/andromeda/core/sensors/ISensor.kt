package com.kylecorry.andromeda.core.sensors

import com.kylecorry.luna.topics.ITopic
import com.kylecorry.luna.topics.Subscriber
import java.time.Instant
import kotlin.time.toKotlinInstant

interface ISensor : ITopic, Sensor {

    override val quality: Quality

    override val hasValidReading: Boolean

    /**
     * The time of the latest reading, in nanoseconds. 0 if there hasn't been a reading.
     */
    override val eventTimeElapsedNanos: Long

    /**
     * The wall clock time of the latest reading, or Instant.EPOCH if there hasn't been a reading
     */
    val eventTime: Instant

    override val timestamp: kotlin.time.Instant get() = eventTime.toKotlinInstant()

    override fun start(subscriber: Subscriber)

    override fun stop(subscriber: Subscriber?)
}
