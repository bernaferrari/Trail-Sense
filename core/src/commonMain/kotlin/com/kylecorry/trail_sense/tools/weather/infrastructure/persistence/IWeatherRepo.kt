package com.kylecorry.trail_sense.tools.weather.infrastructure.persistence

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import kotlinx.coroutines.flow.SharedFlow

/** The stored weather (pressure, temperature, humidity) readings. */
interface IWeatherRepo : IReadingRepo<RawWeatherObservation> {
    /** Emits after readings are added, changed or removed. */
    val readingsChanged: SharedFlow<Unit>

    suspend fun getLast(): Reading<RawWeatherObservation>?

    /** Adds new readings (id 0) and updates the rest. */
    suspend fun addAll(value: List<Reading<RawWeatherObservation>>)

    suspend fun replaceAll(value: List<Reading<RawWeatherObservation>>)
}
