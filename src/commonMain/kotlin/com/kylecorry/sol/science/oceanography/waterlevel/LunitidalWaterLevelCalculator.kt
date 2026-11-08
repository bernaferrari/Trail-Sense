package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.science.astronomy.MoonCalculationModel
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class LunitidalWaterLevelCalculator(
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

    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        val previous = closestTide(time, timeZone, false) ?: return 0f
        val next = closestTide(time, timeZone, true) ?: return 0f
        check(previous != next)
        val bounds = if (previous.isHigh == next.isHigh) {
            val low = previous.time + (next.time - previous.time) / 2
            if (low < time) {
                Tide.low(low, waterLevelRange?.start, timeZone) to Tide.high(next.time, waterLevelRange?.end, timeZone)
            } else {
                Tide.high(previous.time, waterLevelRange?.end, timeZone) to Tide.low(low, waterLevelRange?.start, timeZone)
            }
        } else previous to next
        val value = RuleOfTwelfthsWaterLevelCalculator(bounds.first, bounds.second).calculate(time, timeZone)
            .coerceIn(waterLevelRange?.start ?: -1f, waterLevelRange?.end ?: 1f)
        check(value.isFinite())
        return value
    }

    private fun closestTide(time: Instant, zone: TimeZone, isNext: Boolean): Tide? {
        val previousHigh = closest(time, tideTimes(time, zone, lunitidalInterval), false)
        val nextHigh = closest(time, tideTimes(time, zone, lunitidalInterval), true)
        val high = if (lowLunitidalInterval != null && isNext) nextHigh else previousHigh
        val low = if (lowLunitidalInterval != null) {
            closest(time, tideTimes(time, zone, lowLunitidalInterval), isNext)
        } else if (previousHigh != null && nextHigh != null) {
            previousHigh + (nextHigh - previousHigh) / 2
        } else null
        val selected = closest(time, listOf(high, low), isNext) ?: return null
        return if (selected == high) Tide.high(selected, waterLevelRange?.end, zone)
        else Tide.low(selected, waterLevelRange?.start, zone)
    }

    private fun tideTimes(time: Instant, zone: TimeZone, interval: Duration): List<Instant> {
        val tides = moonTransitCache.toList().map { it + interval }.toMutableList()
        if (tides.none { it > time - 14.hours && it < time }) populate(time, zone, tides, interval, true)
        if (tides.none { it > time && it < time + 14.hours }) populate(time, zone, tides, interval, false)
        return tides
    }

    private fun populate(
        start: Instant, zone: TimeZone, tides: MutableList<Instant>, interval: Duration, before: Boolean
    ) {
        val direction = if (before) -1 else 1
        repeat(2) { index ->
            val time = start.plus(direction * index, DateTimeUnit.DAY, zone)
            val upper = MoonCalculationModel.events(time, zone, location, false, false).transit
            val lowerZone = approximateZone(antipode)
            val lower = MoonCalculationModel.events(time, lowerZone, antipode, false, false).transit
            for (transit in listOfNotNull(upper, lower)) {
                val tide = transit + interval
                if (tide !in tides) {
                    tides.add(tide)
                    moonTransitCache.add(transit)
                }
            }
            val candidate = closest(start, listOf(upper?.plus(interval), lower?.plus(interval)), !before)
            if (candidate != null && candidate > start - 14.hours && candidate < start + 14.hours) return
        }
    }

    private fun closest(time: Instant, candidates: List<Instant?>, next: Boolean): Instant? =
        candidates.filterNotNull().filter { if (next) it > time else it < time }
            .minByOrNull { (it - time).absoluteValue }

    private fun approximateZone(location: Coordinate): TimeZone {
        val offset = (location.longitude / 15).roundToInt()
        val sign = if (offset >= 0) "+" else "-"
        return TimeZone.of("UTC$sign${offset.absoluteValue.toString().padStart(2, '0')}:00")
    }
}
