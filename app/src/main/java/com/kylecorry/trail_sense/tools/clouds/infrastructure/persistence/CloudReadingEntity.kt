package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence

import kotlin.time.toJavaInstant

import com.kylecorry.trail_sense.shared.time.interop.*

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import java.time.Instant

@Entity(tableName = "clouds", indices = [Index(value = ["time"])])
data class CloudReadingEntity(
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "genus") val genus: CloudGenus?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toReading(): Reading<CloudObservation> {
        return Reading(CloudObservation(id, genus), time)
    }

    companion object {
        fun from(reading: Reading<CloudObservation>): CloudReadingEntity {
            return CloudReadingEntity(reading.time.toJavaInstant(), reading.value.genus).also {
                it.id = reading.value.id
            }
        }
    }

}
