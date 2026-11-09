package com.kylecorry.andromeda.sense.mock

import com.kylecorry.andromeda.sense.barometer.Barometer

class MockBarometer(
    private val mockPressure: Float = 1013.25f,
    interval: Long = 0
) : MockSensor(interval), Barometer {
    override val pressure: Float
        get() = mockPressure
}