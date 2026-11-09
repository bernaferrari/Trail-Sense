package com.kylecorry.andromeda.battery

import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.Thermometer
interface Battery : Sensor, Thermometer {
    val percent: Float
    val capacity: Float
    val maxCapacity: Float
    val health: BatteryHealth
    val voltage: Float
    val current: Float
    val chargingMethod: BatteryChargingMethod
    val chargingStatus: BatteryChargingStatus
}