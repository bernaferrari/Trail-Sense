package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface ITemperatureRepo {
    suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>>

    suspend fun getTemperatures(
        location: Coordinate,
        start: Instant,
        end: Instant
    ): List<Reading<Temperature>>

    suspend fun getTemperature(location: Coordinate, time: Instant): Temperature
    suspend fun getDailyTemperatureRange(location: Coordinate, date: LocalDate): Range<Temperature>
}
