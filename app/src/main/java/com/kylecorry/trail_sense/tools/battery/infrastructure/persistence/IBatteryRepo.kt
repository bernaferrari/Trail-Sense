package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import kotlin.time.Instant

interface IBatteryRepo {
    fun get(): Flow<List<BatteryReading>>
    suspend fun add(reading: BatteryReading)
    suspend fun deleteBefore(time: Instant)
    fun getMaxCapacity(): Float
}
