package com.kylecorry.andromeda.sense

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.tryOrLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public suspend fun readAll(
    sensors: List<Sensor>,
    timeout: Duration = 1.minutes,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onlyIfInvalid: Boolean = false,
    forceStopOnCompletion: Boolean = false
): Unit = withContext(dispatcher) {
    try {
        withTimeoutOrNull(timeout.inWholeMilliseconds) {
            val jobs = mutableListOf<Job>()
            for (sensor in sensors) {
                if (!onlyIfInvalid || !sensor.hasValidReading) {
                    jobs.add(launch { sensor.read() })
                }
            }
            jobs.joinAll()
        }
    } finally {
        if (forceStopOnCompletion) {
            sensors.forEach {
                tryOrLog {
                    it.stop(null)
                }
            }
        }
    }
}