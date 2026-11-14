package com.kylecorry.andromeda.sense.mock

import com.kylecorry.andromeda.core.sensors.SharedSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

public abstract class MockSensor(private val interval: Long = 0) : SharedSensor() {
    init { require(interval >= 0) }
    override val hasValidReading: Boolean = true
    private var job: Job? = null

    override fun startImpl() {
        job = CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
            do {
                setEventTime(Clock.System.now())
                notifyListeners()
                if (interval == 0L) break
                delay(interval)
            } while (isActive)
        }
        job?.start()
    }

    override fun stopImpl() {
        job?.cancel()
        job = null
    }
}
