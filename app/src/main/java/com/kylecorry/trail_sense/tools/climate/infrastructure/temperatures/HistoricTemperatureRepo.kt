package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import kotlin.time.Duration.Companion.minutes

import com.kylecorry.sol.time.Time
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.climate.infrastructure.MonthlyValueInterpolator
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators.DailyTemperatureCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Instant
import kotlin.math.max
import kotlin.math.min

internal class HistoricTemperatureRepo() : ITemperatureRepo {

    private val highInterpolator = MonthlyValueInterpolator()
    private val lowInterpolator = MonthlyValueInterpolator()

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val monthly =
            HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(location)

        Time.getYearlyValues(year) {
            getDailyRange(location, it, monthly)
        }
    }

    override suspend fun getTemperatures(
        location: Coordinate,
        start: Instant,
        end: Instant
    ): List<Reading<Temperature>> {
        val calculator = DailyTemperatureCalculator(location, this::getDailyRange)

        return Time.getReadings(start, end, 10.minutes) {
            calculator.calculate(it)
        }
    }

    override suspend fun getTemperature(location: Coordinate, time: Instant): Temperature {
        val calculator = DailyTemperatureCalculator(location, this::getDailyRange)
        return calculator.calculate(time)
    }

    override suspend fun getDailyTemperatureRange(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        return getDailyRange(location, date)
    }

    private suspend fun getDailyRange(
        location: Coordinate,
        date: LocalDate,
        monthlyRanges: Map<Month, Range<Temperature>>? = null
    ): Range<Temperature> = onDefault {
        val months =
            monthlyRanges ?: HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(
                location
            )

        val lowMonths = months.mapValues {
            it.value.start.celsius().value
        }

        val highMonths = months.mapValues {
            it.value.end.celsius().value
        }

        val low = lowInterpolator.interpolate(date, lowMonths)
        val high = highInterpolator.interpolate(date, highMonths)

        Range(Temperature.celsius(min(low, high)), Temperature.celsius(max(low, high)))
    }
}
