package com.kylecorry.sol.science.oceanography

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.oceanography.waterlevel.*
import com.kylecorry.sol.tests.ZonedTime
import com.kylecorry.sol.tests.atZone
import com.kylecorry.sol.tests.zoned
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.roundNearestMinute
import com.kylecorry.sol.units.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class OceanographyTest {

    private val newYork = TimeZone.of("America/New_York")

    @Test
    fun getLunitidalInterval() {
        val location = Coordinate(42.0, -72.0)
        val time = zoned(2026, 4, 17, 14, 53, zone = newYork)

        val interval = Oceanography.getLunitidalInterval(time.instant, location, time.zone)
        assertEquals(120L, interval?.inWholeMinutes)
    }

    @Test
    fun getMeanLunitidalIntervalAveragesToThirtyMinutesForApril2026ReferenceTides() {
        val location = Coordinate(42.0, -72.0)
        val highs = listOf(
            zoned("2026-04-13T10:06:00-04:00[America/New_York]"),
            zoned("2026-04-13T22:34:00-04:00[America/New_York]"),
            zoned("2026-04-14T10:55:00-04:00[America/New_York]"),
            zoned("2026-04-14T23:23:00-04:00[America/New_York]"),
        )

        val interval = Oceanography.getMeanLunitidalInterval(highs.map { it.instant }, location, highs.first().zone)

        assertEquals(30L, interval?.inWholeMinutes)
    }

    @Test
    fun getTidalRange() {
        val cases = listOf(
            Pair(LocalDateTime(2020, 9, 13, 6, 0), TidalRange.Neap),
            Pair(LocalDateTime(2020, 9, 17, 6, 0), TidalRange.Spring),
            Pair(LocalDateTime(2020, 9, 8, 6, 0), TidalRange.Normal)
        )

        for (case in cases) {
            val time = case.first.atZone(newYork)
            val tide = Oceanography.getTidalRange(time.instant, time.zone)
            assertEquals(case.second, tide)
        }
    }

    @Test
    fun getTidesHarmonics() {
        val constituents = listOf(
            Triple(1, 0.505968f, 2.3f),
            Triple(2, 0.10668f, 25.0f),
            Triple(3, 0.124968f, 345.8f),
            Triple(4, 0.06096000000000001f, 166.1f),
            Triple(5, 0.057912000000000005f, 35.8f),
            Triple(6, 0.045720000000000004f, 202.0f),
            Triple(7, 0.006096000000000001f, 220.1f),
            Triple(8, 0.009144f, 19.5f),
            Triple(9, 0.006096000000000001f, 5.1f),
            Triple(10, 0.027432f, 347.9f),
            Triple(11, 0.021336000000000004f, 341.0f),
            Triple(12, 0.0f, 222.9f),
            Triple(13, 0.024384000000000003f, 344.5f),
            Triple(14, 0.018288f, 333.0f),
            Triple(15, 0.0030480000000000004f, 195.7f),
            Triple(16, 0.0030480000000000004f, 345.5f),
            Triple(17, 0.006096000000000001f, 121.9f),
            Triple(18, 0.0030480000000000004f, 204.0f),
            Triple(19, 0.006096000000000001f, 181.1f),
            Triple(20, 0.018288f, 73.9f),
            Triple(21, 0.015240000000000002f, 75.1f),
            Triple(22, 0.06096000000000001f, 145.3f),
            Triple(23, 0.0f, 0.0f),
            Triple(24, 0.0f, 0.0f),
            Triple(25, 0.0030480000000000004f, 230.9f),
            Triple(26, 0.012192000000000001f, 185.3f),
            Triple(27, 0.009144f, 9.0f),
            Triple(28, 0.0f, 252.2f),
            Triple(29, 0.0030480000000000004f, 172.9f),
            Triple(30, 0.021336000000000004f, 176.6f),
            Triple(31, 0.0030480000000000004f, 48.8f),
            Triple(32, 0.006096000000000001f, 34.1f),
            Triple(33, 0.012192000000000001f, 349.9f),
            Triple(34, 0.009144f, 357.1f),
            Triple(35, 0.030480000000000004f, 21.7f),
            Triple(36, 0.0f, 330.4f),
            Triple(37, 0.015240000000000002f, 106.4f)
        ).mapNotNull {
            val (order, amplitude, phase) = it
            val constituent = TideConstituent.entries.firstOrNull { constituent -> constituent.id == order.toLong() }
            if (constituent == null) {
                null
            } else {
                TidalHarmonic(constituent, amplitude, phase)
            }
        }

        val start = zoned(2024, 7, 1, zone = newYork)
        val end = zoned(2024, 7, 29, zone = newYork)

        testCalculator("Harmonic", HarmonicWaterLevelCalculator(constituents), start, end, noaaReferenceTides())
    }

    @Test
    fun canGetTides() {
        val location = Coordinate(42.8150, -70.8733)
        val start = zoned(2024, 7, 1, zone = newYork)
        val end = zoned(2024, 7, 29, zone = newYork)
        val source = noaaReferenceTides()

        val highs = source.filter { it.isHigh }
        val lows = source.filter { !it.isHigh }
        val localLunitidalInterval =
            Oceanography.getMeanLunitidalInterval(highs.map { it.time }, location, newYork) ?: Duration.ZERO
        val localLowLunitidalInterval =
            Oceanography.getMeanLunitidalInterval(lows.map { it.time }, location, newYork) ?: Duration.ZERO
        val utcLunitidalInterval =
            Oceanography.getMeanLunitidalInterval(highs.map { it.time }, timeZone = newYork) ?: Duration.ZERO
        val utcLowLunitidalInterval =
            Oceanography.getMeanLunitidalInterval(lows.map { it.time }, timeZone = newYork) ?: Duration.ZERO

        println("Local lunitidal interval: ${Time.hours(localLunitidalInterval).roundPlaces(2)}")
        println("UTC lunitidal interval: ${Time.hours(utcLunitidalInterval).roundPlaces(2)}")
        println("UTC low lunitidal interval: ${Time.hours(utcLowLunitidalInterval).roundPlaces(2)}")
        println()

        val calculators = listOf(
            "LUNITIDAL HARMONIC (LOCAL)" to HarmonicLunitidalWaterLevelCalculator(localLunitidalInterval, location),
            "LUNITIDAL HARMONIC (UTC)" to HarmonicLunitidalWaterLevelCalculator(utcLunitidalInterval),
            "TIDE CLOCK" to TideClockWaterLevelCalculator(Tide.high(highs.first().time, timeZone = newYork), TideConstituent.M2.speed),
            "LUNITIDAL (LOCAL)" to LunitidalWaterLevelCalculator(localLunitidalInterval, location),
            "LUNITIDAL (LOCAL; HIGH AND LOW)" to LunitidalWaterLevelCalculator(
                localLunitidalInterval,
                location,
                localLowLunitidalInterval
            ),
            "LUNITIDAL (UTC)" to LunitidalWaterLevelCalculator(
                utcLunitidalInterval,
                lowLunitidalInterval = utcLowLunitidalInterval
            ),
        )

        calculators.forEach { (name, calculator) ->
            testCalculator(name, calculator, start, end, source)
        }
    }

    private fun noaaReferenceTides(): List<Tide> {
        return NOAA_8452660_JULY_2024.trim().lines().drop(1).map {
            val parts = it.split(",")
            Tide(
                LocalDateTime.parse(parts[0].replace(" ", "T")).atZone(newYork),
                parts[2] == "H",
                parts[1].toFloat()
            )
        }
    }

    private fun testCalculator(
        name: String,
        calculator: IWaterLevelCalculator,
        start: ZonedTime,
        end: ZonedTime,
        references: List<Tide>
    ) {
        val tides = Oceanography.getTides(
            calculator,
            start.instant,
            end.instant,
            start.zone,
            GoldenSearchExtremaFinder(30.0, 1.0)
        )

        val deltas = mutableListOf<Float>()

        for (i in tides.indices) {
            val tide = tides[i].time.roundNearestMinute(timeZone = tides[i].timeZone)
            val refTide =
                references.filter { it.isHigh == tides[i].isHigh }
                    .minBy { (it.time - tide).absoluteValue }
            val d = (refTide.time - tide).absoluteValue
            deltas.add(d.inWholeMinutes.toFloat())
        }

        val average = Statistics.mean(deltas)
        val max = deltas.max()
        val quantile95 = Statistics.quantile(deltas, 0.95f)
        val quantile80 = Statistics.quantile(deltas, 0.8f)
        val quantile50 = Statistics.quantile(deltas, 0.5f)
        println(name)
        println("Average: $average")
        println("Max: $max")
        println("95th percentile: $quantile95")
        println("80th percentile: $quantile80")
        println("50th percentile: $quantile50")
        println()

        assertTrue(average < 35)
        assertTrue(max < 90)
        assertTrue(quantile95 < 70)
        assertTrue(quantile80 < 60)
        assertTrue(quantile50 < 30)
    }

    /**
     * Development helper (not a test): produces a minute-by-minute harmonic water level series as CSV.
     */
    @Suppress("unused")
    private fun generateHarmonicCsv(): String {
        val harmonics = listOf(
            TidalHarmonic(TideConstituent.M2, 1.66f, 2.3f),
            TidalHarmonic(TideConstituent.S2, 0.35f, 25f),
            TidalHarmonic(TideConstituent.N2, 0.41f, 345.8f),
            TidalHarmonic(TideConstituent.K1, 0.2f, 166.1f),
            TidalHarmonic(TideConstituent.O1, 0.15f, 202f),
            TidalHarmonic(TideConstituent.P1, 0.07f, 176.6f),
            TidalHarmonic(TideConstituent.M4, 0.19f, 35.8f),
            TidalHarmonic(TideConstituent.K2, 0.1f, 21.7f),
            TidalHarmonic(TideConstituent.L2, 0.04f, 349.9f),
            TidalHarmonic(TideConstituent.MS4, 0.05f, 106.4f),
            TidalHarmonic(TideConstituent.Z0, 0f, 0f)
        )

        val zone = newYork
        val date = LocalDate(2021, 1, 1)
        val levels = mutableListOf<Float>()
        val calculator = HarmonicWaterLevelCalculator(harmonics)
        var time = date.atStartOfDayIn(zone)
        val end = date.plus(31, DateTimeUnit.DAY).atStartOfDayIn(zone)
        while (time < end) {
            levels.add(calculator.calculate(time, zone))
            time += 1.minutes
        }
        return levels.joinToString(",")
    }

    @Test
    fun canCalculateDepth() {
        val currentPressure = Pressure.hpa(2222.516f)

        val depth = Oceanography.getDepth(currentPressure, Pressure.hpa(1013f))

        val expected = Distance.from(12f, DistanceUnits.Meters)

        assertEquals(expected.value, depth.value, 0.1f)
    }

    @Test
    fun depthReturnsZeroWhenAboveWater() {
        val currentPressure = Pressure.hpa(1000f)

        val depth = Oceanography.getDepth(currentPressure, Pressure.hpa(1013f))

        val expected = Distance.from(0f, DistanceUnits.Meters)

        assertEquals(expected, depth)
    }


    companion object {

        fun provideTides(): List<List<Any?>> {
            val reference1 = time(2021, 3, 15, 10, 7, "America/New_York")
            return listOf(
                listOf(
                    reference1, LocalDate(2021, 3, 16), listOf(
                        Tide.low(time(2021, 3, 16, 4, 31, "America/New_York")),
                        Tide.high(time(2021, 3, 16, 10, 43, "America/New_York")),
                        Tide.low(time(2021, 3, 16, 16, 9, "America/New_York")),
                        Tide.high(time(2021, 3, 16, 22, 57, "America/New_York"))
                    )
                )
            )
        }

        private fun time(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            zone: String
        ): ZonedTime {
            return zoned(year, month, day, hour, minute, 0, zone)
        }

    }

}
