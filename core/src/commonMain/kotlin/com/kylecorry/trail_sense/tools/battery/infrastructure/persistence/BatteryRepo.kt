package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import kotlin.time.Instant

class BatteryRepo(private val batteryDao: BatteryDao, prefs: Preferences) : IBatteryRepo {
    private var maxCapacityPref by FloatPreference(prefs, "pref_max_battery_capacity", 0f)

    override fun get(): Flow<List<BatteryReading>> {
        return batteryDao.get().map { it.map { reading -> reading.toBatteryReading() } }
    }

    override suspend fun add(reading: BatteryReading) {
        onIO {
            batteryDao.insert(BatteryReadingEntity.from(reading))

            // Record the maximum capacity the battery has reached
            if (getMaxCapacity() < reading.capacity) {
                maxCapacityPref = reading.capacity
            }
        }
    }

    override suspend fun deleteBefore(time: Instant) = onIO {
        batteryDao.deleteOlderThan(time)
    }

    override fun getMaxCapacity(): Float {
        return maxCapacityPref
    }

}
