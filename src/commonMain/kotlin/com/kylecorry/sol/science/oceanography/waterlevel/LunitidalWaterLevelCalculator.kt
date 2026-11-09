package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.science.astronomy.MoonCalculationModel
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

public class LunitidalWaterLevelCalculator(
    private val lunitidalInterval: Duration,
    private val location: Coordinate = Coordinate.zero,
    private val lowLunitidalInterval: Duration? = null,
    private val waterLevelRange: Range<Float>? = null
) : IWaterLevelCalculator {

    init {
        require(waterLevelRange == null || waterLevelRange.start <= waterLevelRange.end) {
            "waterLevelRange start must be less than or equal to end."
        }
    }

    private val moonTransitCache = RingBuffer<Instant>(24)
    private val antipode = location.antipode
    private val antipodeZone = Time.getApproximateTimeZone(antipode)

    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        val (previous, next) = surroundingTides(time, timeZone) ?: return 0f
        check(previous != next)

        val (first, second) = if (previous.isHigh == next.isHigh) {
            // Two highs (or lows) in a row: insert the missing tide halfway between them
            val middle = previous.time + (next.time - previous.time) / 2
            if (middle < time) {
                lowTide(middle, timeZone) to highTide(next.time, timeZone)
            } else {
                highTide(previous.time, timeZone) to lowTide(middle, timeZone)
            }
        } else {
            previous to next
        }

        val value = RuleOfTwelfthsWaterLevelCalculator(first, second)
            .calculate(time, timeZone)
            .coerceIn(waterLevelRange?.start ?: -1f, waterLevelRange?.end ?: 1f)
        check(value.isFinite())
        return value
    }

    private fun surroundingTides(time: Instant, zone: TimeZone): Pair<Tide, Tide>? {
        val previous = closestTide(time, zone, isNext = false) ?: return null
        return closestTide(time, zone, isNext = true)?.let { next -> previous to next }
    }

    private fun closestTide(time: Instant, zone: TimeZone, isNext: Boolean): Tide? {
        val highTimes = tideTimes(time, zone, lunitidalInterval)
        val previousHigh = closest(time, highTimes, next = false)
        val nextHigh = closest(time, highTimes, next = true)
        val high = if (lowLunitidalInterval != null && isNext) nextHigh else previousHigh

        val low = when {
            lowLunitidalInterval != null -> closest(time, tideTimes(time, zone, lowLunitidalInterval), isNext)
            previousHigh != null && nextHigh != null -> previousHigh + (nextHigh - previousHigh) / 2
            else -> null
        }

        val selected = closest(time, listOf(high, low), isNext) ?: return null
        return if (selected == high) highTide(selected, zone) else lowTide(selected, zone)
    }

    private fun highTide(time: Instant, zone: TimeZone) = Tide.high(time, waterLevelRange?.end, zone)

    private fun lowTide(time: Instant, zone: TimeZone) = Tide.low(time, waterLevelRange?.start, zone)

    private fun tideTimes(time: Instant, zone: TimeZone, interval: Duration): List<Instant> {
        val tides = moonTransitCache.toList().map { it + interval }.toMutableList()
        if (tides.none { it > time - SEARCH_WINDOW && it < time }) {
            populate(time, zone, tides, interval, before = true)
        }
        if (tides.none { it > time && it < time + SEARCH_WINDOW }) {
            populate(time, zone, tides, interval, before = false)
        }
        return tides
    }

    @Suppress("ReturnCount")
    private fun populate(
        start: Instant,
        zone: TimeZone,
        tides: MutableList<Instant>,
        interval: Duration,
        before: Boolean
    ) {
        val direction = if (before) -1 else 1
        repeat(2) { index ->
            val day = start.plus(direction * index, DateTimeUnit.DAY, zone)
            val upper = MoonCalculationModel.events(day, zone, location, false, false).transit
            val lower = MoonCalculationModel.events(day, antipodeZone, antipode, false, false).transit
            for (transit in listOfNotNull(upper, lower)) {
                val tide = transit + interval
                if (tide !in tides) {
                    tides.add(tide)
                    moonTransitCache.add(transit)
                }
            }

            val candidate = closest(start, listOf(upper?.plus(interval), lower?.plus(interval)), !before)
            if (candidate != null && candidate > start - SEARCH_WINDOW && candidate < start + SEARCH_WINDOW) {
                return
            }
        }
    }

    private fun closest(time: Instant, candidates: List<Instant?>, next: Boolean): Instant? {
        return candidates
            .filterNotNull()
            .filter { if (next) it > time else it < time }
            .minByOrNull { (it - time).absoluteValue }
    }

    private companion object {
        val SEARCH_WINDOW = 14.hours
    }
}
