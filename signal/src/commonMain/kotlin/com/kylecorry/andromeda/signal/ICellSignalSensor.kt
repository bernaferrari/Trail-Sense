package com.kylecorry.andromeda.signal

import com.kylecorry.andromeda.signal.CellSignal

import com.kylecorry.andromeda.core.sensors.Sensor
interface ICellSignalSensor: Sensor {
    val signals: List<CellSignal>
}