package com.kylecorry.trail_sense.tools.tides.domain

import kotlin.time.toJavaDuration

import kotlin.time.Clock

import com.kylecorry.trail_sense.shared.time.toLocalDate

import com.kylecorry.trail_sense.shared.time.atStartOfDay

import com.kylecorry.trail_sense.shared.time.minusDays

import com.kylecorry.trail_sense.shared.time.plusDays

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

import android.content.Context
import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.science.oceanography.Oceanography
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.range.TideTableRangeCalculator
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import com.kylecorry.sol.time.Time

class TideService(private val context: Context) {

    private val maxSearchIterations = 10

    private val locationSubsystem by lazy { LocationSubsystem.getInstance(context) }

    private val locationKeyCache = MemoryCachedValue<Coordinate>(duration = 10.seconds.toJavaDuration())
    private val cache =
        MemoryLRUCache<Pair<TideTable, Coordinate?>, TideTableWaterLevelCalculator>(100)

    suspend fun getTides(
        table: TideTable,
        date: LocalDate,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): List<Tide> {
        val start = date.atStartOfDay(zone)
        val end = date.plusDays(1).atStartOfDay(zone)
        val waterLevelCalculator = getTableCalculator(table)
        val extremaFinder = GoldenSearchExtremaFinder(30.0, 1.0)
        val tides = Oceanography.getTides(
            waterLevelCalculator,
            start,
            end,
            zone,
            extremaFinder
        )
        return tides.filter { it.time.toLocalDate(zone) == date }
    }

    suspend fun getWaterLevel(
        table: TideTable,
        time: Instant,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Float {
        return getTableCalculator(table).calculateSuspend(time, zone)
    }

    suspend fun getWaterLevels(
        table: TideTable,
        date: LocalDate,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): List<Reading<Float>> {
        return Time.getReadings(
            date,
            zone,
            10.minutes
        ) {
            getWaterLevel(table, it, zone)
        }
    }

    suspend fun getRange(table: TideTable): Range<Float> {
        return TideTableRangeCalculator().getRange(table)
    }

    suspend fun isWithinTideTable(
        table: TideTable,
        time: Instant = Clock.System.now()
    ): Boolean {
        val sortedTides = table.tides.sortedBy { it.time }
        for (i in 0 until sortedTides.lastIndex) {
            if (sortedTides[i].time <= time && sortedTides[i + 1].time >= time) {
                val period = sortedTides[i + 1].time - sortedTides[i].time
                val maxPeriod = Time.hours(180 / table.principalFrequency + 3.0)
                return !(sortedTides[i].isHigh == sortedTides[i + 1].isHigh || period > maxPeriod)
            }
        }
        return false
    }

    suspend fun getCurrentTide(
        table: TideTable,
        time: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): TideType? {
        val next = getNextTide(table, time, zone) ?: return null
        val timeToNextTide = next.time - time
        val closeToNextTide = timeToNextTide < 2.hours
        val farFromNextTide = timeToNextTide > 4.hours
        val nextIsHigh = next.isHigh
        val nextIsLow = !next.isHigh
        return if ((nextIsHigh && closeToNextTide) || (nextIsLow && farFromNextTide)) {
            TideType.High
        } else if ((nextIsLow && closeToNextTide) || (nextIsHigh && farFromNextTide)) {
            TideType.Low
        } else {
            null
        }
    }

    suspend fun getPhase(
        table: TideTable,
        time: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Float? {
        // High = 0
        // Falling = 90
        // Low = 180
        // Rising = 270
        val next = getNextTide(table, time, zone) ?: return null
        val previous =
            getPreviousTide(table, time, zone, type = if (next.isHigh) TideType.Low else TideType.High)
                ?: return null
        val total = (next.time - previous.time).inWholeMilliseconds.toDouble()
        val delta = (time - previous.time).inWholeMilliseconds.toDouble()
        val progress = (delta / total).toFloat()
        return if (next.isHigh) {
            Interpolation.map(progress, 0f, 1f, 180f, 270f)
        } else {
            Interpolation.map(progress, 0f, 1f, 0f, 180f)
        }
    }

    suspend fun isRising(
        table: TideTable,
        time: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        return getNextTide(table, time, zone)?.isHigh ?: false
    }

    suspend fun getLocation(table: TideTable): Coordinate? {
        val calculator = getTableCalculator(table)
        return calculator.location
    }

    private suspend fun getNextTide(
        table: TideTable,
        time: Instant,
        zone: TimeZone,
        iteration: Int = 0,
        type: TideType? = null
    ): Tide? {
        if (iteration >= maxSearchIterations) {
            return null
        }

        val isTypeHigh = if (type == null) {
            null
        } else {
            type == TideType.High
        }

        val date = time.toLocalDate(zone)
        val todayTides = getTides(table, date, zone)
        val next =
            todayTides.firstOrNull { it.time >= time && (isTypeHigh == null || it.isHigh == isTypeHigh) }
        return next ?: getNextTide(
            table,
            date.plusDays(1).atStartOfDay(zone),
            zone,
            iteration + 1,
            type
        )
    }

    private suspend fun getPreviousTide(
        table: TideTable,
        time: Instant,
        zone: TimeZone,
        iteration: Int = 0,
        type: TideType? = null
    ): Tide? {
        if (iteration >= maxSearchIterations) {
            return null
        }

        val isTypeHigh = if (type == null) {
            null
        } else {
            type == TideType.High
        }

        val date = time.toLocalDate(zone)
        val todayTides = getTides(table, date, zone)
        val previous =
            todayTides.lastOrNull { it.time <= time && (isTypeHigh == null || it.isHigh == isTypeHigh) }
        return previous ?: getPreviousTide(
            table,
            date.minusDays(1).atStartOfDay(zone),
            zone,
            iteration + 1,
            type
        )
    }

    private suspend fun getTableCalculator(table: TideTable): TideTableWaterLevelCalculator {
        // Automatic nearby tides require on the fly locations rather than the location being part of the model
        val key = table to if (table.isAutomaticNearbyTide) {
            locationKeyCache.getOrPut { TideLocationKey.of(locationSubsystem.location) }
        } else {
            null
        }
        return cache.getOrPut(key) {
            val calculator = TideTableWaterLevelCalculator(context, table)
            // Precompute to populate all lookups
            calculator.calculateSuspend(kotlin.time.Clock.System.now())
            calculator
        }
    }
}
