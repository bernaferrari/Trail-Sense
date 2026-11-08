package com.kylecorry.andromeda.sense.mock

import com.kylecorry.andromeda.sense.barometer.IBarometer

class MockBarometer(
    private val mockPressure: Float = 1013.25f,
    interval: Long = 0
) : MockSensor(interval), IBarometer {
    override val pressure: Float
        get() = mockPressure
}