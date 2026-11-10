package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.sol.units.Bearing

class MockCompass : MockSensor(), CompassSensor {
    override val bearing: Bearing = Bearing.from(0f)

    override var declination: Float = 0f

    override val rawBearing: Float = 0f
}
