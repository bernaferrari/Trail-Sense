package com.kylecorry.andromeda.signal

import com.kylecorry.andromeda.signal.CellSignal

import com.kylecorry.andromeda.core.sensors.Sensor
public interface CellSignalSensor: Sensor {
    public val signals: List<CellSignal>
}