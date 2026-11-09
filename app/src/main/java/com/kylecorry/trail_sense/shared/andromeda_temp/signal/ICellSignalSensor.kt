package com.kylecorry.trail_sense.shared.andromeda_temp.signal

import com.kylecorry.andromeda.signal.CellSignal

import com.kylecorry.andromeda.core.sensors.Sensor
interface ICellSignalSensor: Sensor {
    val signals: List<CellSignal>
}