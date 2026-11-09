package com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures

import kotlin.time.Instant

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.sensors.thermometer.ITemperatureCalibrator
import kotlinx.datetime.LocalDate

class CalibratedTemperatureService(
    private val service: ITemperatureService,
    private val calibrator: ITemperatureCalibrator
) : ITemperatureService {
    override suspend fun getTemperature(time: Instant): Temperature {
        return calibrator.calibrate(service.getTemperature(time))
    }

    override suspend fun getTemperatures(
        start: Instant,
        end: Instant
    ): List<Reading<Temperature>> {
        return service.getTemperatures(start, end).map {
            it.copy(value = calibrator.calibrate(it.value))
        }
    }

    override suspend fun getTemperatureRange(date: LocalDate): Range<Temperature> {
        val range = service.getTemperatureRange(date)
        return Range(
            calibrator.calibrate(range.start),
            calibrator.calibrate(range.end),
        )
    }

    override suspend fun getTemperatureRange(
        start: Instant,
        end: Instant
    ): Range<Temperature> {
        val range = service.getTemperatureRange(start, end)
        return Range(
            calibrator.calibrate(range.start),
            calibrator.calibrate(range.end),
        )
    }

    override suspend fun getTemperatureRanges(year: Int): List<Pair<LocalDate, Range<Temperature>>> {
        return service.getTemperatureRanges(year).map {
            it.copy(
                second = Range(
                    calibrator.calibrate(it.second.start),
                    calibrator.calibrate(it.second.end),
                )
            )
        }
    }
}
