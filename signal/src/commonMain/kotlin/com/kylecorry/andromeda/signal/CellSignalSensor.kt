package com.kylecorry.andromeda.signal

import com.kylecorry.andromeda.signal.CellSignal

import com.kylecorry.andromeda.core.sensors.Sensor
interface CellSignalSensor: Sensor {
    val signals: List<CellSignal>
}