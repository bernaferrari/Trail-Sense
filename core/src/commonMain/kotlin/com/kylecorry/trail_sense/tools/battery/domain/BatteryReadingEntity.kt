package com.kylecorry.trail_sense.tools.battery.domain

import kotlin.time.Duration.Companion.milliseconds

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "battery", indices = [Index(value = ["time"])])
class BatteryReadingEntity(
    @ColumnInfo(name = "percent") val percent: Float,
    @ColumnInfo(name = "capacity") val capacity: Float,
    @ColumnInfo(name = "isCharging") val isCharging: Boolean,
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "uptime") val uptime: Long? = null,
    @ColumnInfo(name = "elapsed_realtime") val elapsedRealtime: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toBatteryReading(): BatteryReading {
        return BatteryReading(
            time,
            percent,
            capacity,
            isCharging,
            uptime?.let { it.milliseconds },
            elapsedRealtime?.let { it.milliseconds }
        )
    }

    companion object {
        fun from(reading: BatteryReading): BatteryReadingEntity {
            return BatteryReadingEntity(
                reading.percent,
                reading.capacity,
                reading.isCharging,
                reading.time,
                reading.uptime?.inWholeMilliseconds,
                reading.elapsedRealtime?.inWholeMilliseconds
            )
        }
    }

}
