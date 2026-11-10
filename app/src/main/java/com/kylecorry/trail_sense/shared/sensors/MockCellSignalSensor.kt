package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.signal.CellSignalSensor
class MockCellSignalSensor : MockSensor(20), CellSignalSensor {
    override val signals: List<CellSignal>
        get() = listOf()
}
