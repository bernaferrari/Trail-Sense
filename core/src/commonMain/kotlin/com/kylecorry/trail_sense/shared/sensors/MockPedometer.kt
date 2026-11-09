package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.andromeda.sense.pedometer.Pedometer
class MockPedometer : MockSensor(), Pedometer {
    override val steps: Int = 0
}
