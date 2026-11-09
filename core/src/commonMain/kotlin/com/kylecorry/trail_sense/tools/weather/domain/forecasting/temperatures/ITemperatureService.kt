package com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures

import kotlin.time.Instant

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import kotlinx.datetime.LocalDate

interface ITemperatureService {
    suspend fun getTemperature(time: Instant): Temperature
    suspend fun getTemperatures(
        start: Instant,
        end: Instant
    ): List<Reading<Temperature>>

    suspend fun getTemperatureRange(date: LocalDate): Range<Temperature>
    suspend fun getTemperatureRanges(year: Int): List<Pair<LocalDate, Range<Temperature>>>
    suspend fun getTemperatureRange(start: Instant, end: Instant): Range<Temperature>
}
