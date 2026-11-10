package com.kylecorry.trail_sense.tools.battery.infrastructure.commands

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import com.kylecorry.andromeda.core.sensors.read

import android.content.Context
import android.os.SystemClock
import com.kylecorry.andromeda.battery.AndroidBattery
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BatteryLogCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() {
        val battery = AndroidBattery(context)
        val batteryRepo = BatteryRepo.getInstance(context)
        withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(10.seconds.inWholeMilliseconds) {
                    battery.read()
                }
            } finally {
                battery.stop(null)
            }
        }
        val pct = battery.percent
        val charging = battery.chargingStatus == BatteryChargingStatus.Charging
        val time = Clock.System.now()
        val capacity = battery.capacity
        val reading = BatteryReading(
            time,
            pct,
            capacity,
            charging,
            (SystemClock.uptimeMillis()).milliseconds,
            (SystemClock.elapsedRealtime()).milliseconds
        )
        if (battery.hasValidReading) {
            batteryRepo.add(reading)
        }
        batteryRepo.deleteBefore(Clock.System.now().minus(1.days))
    }
}
