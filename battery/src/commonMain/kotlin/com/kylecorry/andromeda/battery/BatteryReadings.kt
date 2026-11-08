package com.kylecorry.andromeda.battery

import com.kylecorry.andromeda.device.BatteryReading
import com.kylecorry.andromeda.device.DeviceSensors
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow

/** Observes battery percentage and charging state when available. */
fun batteryReadings(): Flow<Reading<BatteryReading>> = DeviceSensors().battery()
