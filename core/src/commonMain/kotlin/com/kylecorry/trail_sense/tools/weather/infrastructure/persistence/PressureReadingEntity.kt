package com.kylecorry.trail_sense.tools.weather.infrastructure.persistence


import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import kotlin.time.Instant

@Entity(
    tableName = "pressures",
    indices = [Index(value = ["time"])]
)
data class PressureReadingEntity(
    @ColumnInfo(name = "pressure") val pressure: Float,
    @ColumnInfo(name = "altitude") val altitude: Float,
    @ColumnInfo(name = "altitude_accuracy") val altitudeAccuracy: Float?,
    @ColumnInfo(name = "temperature") val temperature: Float,
    @ColumnInfo(name = "humidity") val humidity: Float,
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toWeatherObservation(): Reading<RawWeatherObservation> {
        return Reading(
            RawWeatherObservation(
                id,
                pressure,
                altitude,
                temperature,
                altitudeAccuracy,
                humidity,
                Coordinate(latitude, longitude)
            ),
            Instant.fromEpochMilliseconds(time)
        )
    }

    companion object {
        fun from(reading: Reading<RawWeatherObservation>): PressureReadingEntity {
            return PressureReadingEntity(
                reading.value.pressure,
                reading.value.altitude,
                reading.value.altitudeError,
                reading.value.temperature,
                reading.value.humidity ?: 0f,
                reading.time.toEpochMilliseconds(),
                reading.value.location.latitude,
                reading.value.location.longitude
            ).also {
                it.id = reading.value.id
            }
        }
    }

}
