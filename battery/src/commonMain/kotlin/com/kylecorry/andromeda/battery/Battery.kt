package com.kylecorry.andromeda.battery

import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.Thermometer
public interface Battery : Sensor, Thermometer {
    public val percent: Float
    public val capacity: Float
    public val maxCapacity: Float
    public val health: BatteryHealth
    public val voltage: Float
    public val current: Float
    public val chargingMethod: BatteryChargingMethod
    public val chargingStatus: BatteryChargingStatus
}