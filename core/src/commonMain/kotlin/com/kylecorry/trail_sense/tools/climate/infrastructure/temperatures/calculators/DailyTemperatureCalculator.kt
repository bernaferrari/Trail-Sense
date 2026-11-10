package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time.getClosestFutureTime
import com.kylecorry.sol.time.Time.getClosestPastTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.time.atStartOfDay
import com.kylecorry.trail_sense.shared.time.minusDays
import com.kylecorry.trail_sense.shared.time.plusDays
import com.kylecorry.trail_sense.shared.time.systemTimeZone
import com.kylecorry.trail_sense.shared.time.toLocalDate
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DailyTemperatureCalculator(
    private val location: Coordinate,
    private val dailyTemperatureRangeProvider: suspend (location: Coordinate, date: LocalDate) -> Range<Temperature>,
    private val zone: TimeZone = systemTimeZone
) : ITemperatureCalculator {

    private val calculators = mutableListOf<Pair<Range<Instant>, ITemperatureCalculator>>()
    private val offset = 3.hours
    private val astronomy = AstronomyService(timeZone = zone)

    override suspend fun calculate(time: Instant): Temperature {
        val existing = calculators.firstOrNull { it.first.contains(time) }
        val calculator = if (existing == null) {
            val newCalculator = createCalculator(time)
            calculators.add(newCalculator)
            newCalculator.second
        } else {
            existing.second
        }
        return calculator.calculate(time)
    }

    private fun Instant.date(): LocalDate = toLocalDate(zone)

    private suspend fun createCalculator(time: Instant): Pair<Range<Instant>, ITemperatureCalculator> {
        val date = time.date()
        val range = dailyTemperatureRangeProvider.invoke(location, date)
        val nextHigh = getNextHighTime(time)
        val nextLow = getNextLowTime(time)
        val previousHigh = getPreviousHighTime(time)
        val previousLow = getPreviousLowTime(time)

        // Low High x Low High
        if (nextLow < nextHigh) {

            val start = if (nextLow.date() != date) {
                dailyTemperatureRangeProvider.invoke(location, nextLow.date()).start
            } else {
                range.start
            }

            val end = if (previousHigh.date() != date) {
                dailyTemperatureRangeProvider.invoke(location, previousHigh.date()).end
            } else {
                range.end
            }

            return Range(previousHigh, nextLow) to QuadraticTemperatureCalculator(
                Reading(start, nextLow),
                Reading(end, previousHigh)
            )
        }

        val start = if (previousLow.date() != date) {
            dailyTemperatureRangeProvider.invoke(location, previousLow.date()).start
        } else {
            range.start
        }

        val end = if (nextHigh.date() != date) {
            dailyTemperatureRangeProvider.invoke(location, nextHigh.date()).end
        } else {
            range.end
        }

        // High Low x High Low
        return Range(previousLow, nextHigh) to SineTemperatureCalculator(
            Reading(start, previousLow),
            Reading(end, nextHigh)
        )
    }

    private fun LocalDate.atNoonPlusOffset(): Instant = atTime(12, 0).toInstant(zone) + offset

    private fun getNextHighTime(time: Instant): Instant {
        val date = time.date()
        val today = astronomy.getSunTimes(location, SunTimesMode.Actual, date)
        val tomorrow = astronomy.getSunTimes(location, SunTimesMode.Actual, date.plusDays(1))
        return getClosestFutureTime(
            time,
            listOf(today.transit?.plus(offset), tomorrow.transit?.plus(offset))
        ) ?: getClosestFutureTime(
            time,
            listOf(date.atNoonPlusOffset(), date.plusDays(1).atNoonPlusOffset())
        )!!
    }

    private fun getNextLowTime(time: Instant): Instant {
        val date = time.date()
        return astronomy.getNextSunrise(location, SunTimesMode.Actual, time)
            ?: getClosestFutureTime(
                time,
                listOf(date.atStartOfDay(zone), date.plusDays(1).atStartOfDay(zone))
            )!!
    }

    private fun getPreviousHighTime(time: Instant): Instant {
        val date = time.date()
        val today = astronomy.getSunTimes(location, SunTimesMode.Actual, date)
        val yesterday = astronomy.getSunTimes(location, SunTimesMode.Actual, date.minusDays(1))
        return getClosestPastTime(
            time,
            listOf(today.transit?.plus(offset), yesterday.transit?.plus(offset))
        ) ?: getClosestPastTime(
            time,
            listOf(date.atNoonPlusOffset(), date.minusDays(1).atNoonPlusOffset())
        )!!
    }

    private fun getPreviousLowTime(time: Instant): Instant {
        val date = time.date()
        val today = astronomy.getSunTimes(location, SunTimesMode.Actual, date)
        val yesterday = astronomy.getSunTimes(location, SunTimesMode.Actual, date.minusDays(1))
        return getClosestPastTime(
            time,
            listOf(today.rise, yesterday.rise)
        ) ?: getClosestPastTime(
            time,
            listOf(date.atStartOfDay(zone), date.minusDays(1).atStartOfDay(zone))
        )!!
    }
}
