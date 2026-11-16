package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence



import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import kotlin.time.Instant

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
            return CloudReadingEntity(reading.time, reading.value.genus).also {
                it.id = reading.value.id
            }
        }
    }

}
