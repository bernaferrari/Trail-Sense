package com.kylecorry.trail_sense.tools.climate.infrastructure

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.data.SharedEncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.SharedGeographicImageSource
import com.kylecorry.trail_sense.shared.data.SharedImageSize
import com.kylecorry.trail_sense.shared.data.SharedPixelDecoders
import com.kylecorry.trail_sense.shared.time.length
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.ITemperatureRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators.DailyTemperatureCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SharedClimateRepo : ITemperatureRepo {

    private val tempCache = MemoryLRUCache<PixelCoordinate, Map<Month, Range<Temperature>>>(size = 5)
    private val precipCache = MemoryLRUCache<PixelCoordinate, Map<Month, Distance>>(size = 5)

    private val tempSize = SharedImageSize(576, 361)
    private val tempExtensions = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )

    private val lowSources = tempExtensions.map { (ext, months) ->
        months to SharedGeographicImageSource(
            SharedEncodedDataImageReader(
                imageSize = tempSize,
                assetPath = "temperatures/T2MMIN-$ext.webp",
                maxChannels = 3,
                decoder = SharedPixelDecoders.scaledDecoder(1.4010988473892212, 56.0)
            ),
            verticalResolutionDegrees = 0.5
        )
    }

    private val highSources = tempExtensions.map { (ext, months) ->
        months to SharedGeographicImageSource(
            SharedEncodedDataImageReader(
                imageSize = tempSize,
                assetPath = "temperatures/T2MMAX-$ext.webp",
                maxChannels = 3,
                decoder = SharedPixelDecoders.scaledDecoder(1.3492063283920288, 48.0)
            ),
            verticalResolutionDegrees = 0.5
        )
    }

    private val precipSize = SharedImageSize(360, 180)
    private val precipSources = Month.entries.associateWith { month ->
        SharedGeographicImageSource(
            SharedEncodedDataImageReader(
                imageSize = precipSize,
                assetPath = "precipitation/precipitation-${month.number}.webp",
                maxChannels = 1,
                decoder = SharedPixelDecoders.split16BitDecoder()
            ),
            interpolationOrder = 0
        )
    }

    private val highInterpolator = MonthlyValueInterpolator()
    private val lowInterpolator = MonthlyValueInterpolator()

    suspend fun getMonthlyPrecipitation(location: Coordinate): Map<Month, Distance> = onIO {
        val pixel = precipSources[Month.JANUARY]!!.getPixel(location)
        precipCache.getOrPut(pixel) {
            Month.entries.associateWith { month ->
                val raw = precipSources[month]!!.read(location).firstOrNull() ?: 0f
                val dailyMm = raw / 30f
                val days = month.length(leapYear = false)
                Distance.from(dailyMm * days, DistanceUnits.Millimeters).meters()
            }
        }
    }

    suspend fun getMonthlyTemperatureRanges(location: Coordinate): Map<Month, Range<Temperature>> = onIO {
        val pixel = lowSources.first().second.getPixel(location)
        tempCache.getOrPut(pixel) {
            val lows = mutableMapOf<Month, Float>()
            for ((months, source) in lowSources) {
                val data = source.read(location)
                lows[months.first] = data[0]
                lows[months.second] = data[1]
                lows[months.third] = data[2]
            }
            val highs = mutableMapOf<Month, Float>()
            for ((months, source) in highSources) {
                val data = source.read(location)
                highs[months.first] = data[0]
                highs[months.second] = data[1]
                highs[months.third] = data[2]
            }
            Month.entries.associateWith { month ->
                val low = lows[month] ?: 0f
                val high = highs[month] ?: 0f
                Range(
                    Temperature.from(min(low, high), TemperatureUnits.Fahrenheit).celsius(),
                    Temperature.from(max(low, high), TemperatureUnits.Fahrenheit).celsius()
                )
            }
        }
    }

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val monthly = getMonthlyTemperatureRanges(location)
        Time.getYearlyValues(year) { date -> getDailyRange(location, date, monthly) }
    }

    override suspend fun getTemperatures(
        location: Coordinate,
        start: Instant,
        end: Instant
    ): List<Reading<Temperature>> {
        val calculator = DailyTemperatureCalculator(location, ::getDailyTemperatureRange)
        return Time.getReadings(start, end, 10.minutes) { calculator.calculate(it) }
    }

    override suspend fun getTemperature(location: Coordinate, time: Instant): Temperature {
        return DailyTemperatureCalculator(location, ::getDailyTemperatureRange).calculate(time)
    }

    override suspend fun getDailyTemperatureRange(location: Coordinate, date: LocalDate): Range<Temperature> {
        return getDailyRange(location, date)
    }

    private suspend fun getDailyRange(
        location: Coordinate,
        date: LocalDate,
        monthlyRanges: Map<Month, Range<Temperature>>? = null
    ): Range<Temperature> = onDefault {
        val months = monthlyRanges ?: getMonthlyTemperatureRanges(location)
        val lowMonths = months.mapValues { it.value.start.celsius().value }
        val highMonths = months.mapValues { it.value.end.celsius().value }
        val low = lowInterpolator.interpolate(date, lowMonths)
        val high = highInterpolator.interpolate(date, highMonths)
        Range(Temperature.celsius(min(low, high)), Temperature.celsius(max(low, high)))
    }
}
