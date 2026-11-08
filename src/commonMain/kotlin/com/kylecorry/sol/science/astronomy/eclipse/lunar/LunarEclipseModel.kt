package com.kylecorry.sol.science.astronomy.eclipse.lunar

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic.power
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.science.astronomy.MoonCalculationModel
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal data class LunarEclipseResult(
    val start: Instant, val end: Instant, val magnitude: Float, val obscuration: Float, val maximum: Instant
)

internal class LunarEclipseModel(private val total: Boolean) {
    fun next(after: Instant, location: Coordinate, zone: TimeZone): LunarEclipseResult? {
        var search = after - 20.days
        repeat(100) {
            val parameters = LunarEclipseParameterModel().getNextLunarEclipseModelParameters(search)
            val magnitude = (LUNAR_ECLIPSE_UMBRA_OUTER_CONTACT_RADIUS - parameters.umbralConeRadius -
                parameters.minDistanceFromCenter.absoluteValue) / MOON_DIAMETER
            if (magnitude >= if (total) 1.0 else 0.0) {
                val contact = (if (total) LUNAR_ECLIPSE_UMBRA_INNER_CONTACT_RADIUS
                    else LUNAR_ECLIPSE_UMBRA_OUTER_CONTACT_RADIUS) - parameters.umbralConeRadius
                val duration = ((60 / parameters.n) * sqrt(power(contact, 2) -
                    power(parameters.minDistanceFromCenter, 2)) * 60).toLong().seconds
                val start = parameters.maximum - duration
                val end = parameters.maximum + duration
                if (end > after) {
                    val result = visibleEclipse(start, end, parameters, magnitude, location, zone)
                    if (result != null) return result
                }
            }
            search = parameters.maximum + 10.days
        }
        return null
    }

    @Suppress("LongParameterList", "ReturnCount")
    private fun visibleEclipse(
        start: Instant, end: Instant, parameters: LunarEclipseModelParameters, magnitude: Double,
        location: Coordinate, zone: TimeZone
    ): LunarEclipseResult? {
        fun isUp(time: Instant) = MoonCalculationModel.position(time, location, true, true).altitude > 0
        val upAtStart = isUp(start)
        val upAtEnd = isUp(end)
        if (!upAtStart && !upAtEnd) return null
        val moon = Circle(Vector2.zero, (MOON_DIAMETER / 2).toFloat())
        val umbra = Circle(
            Vector2(parameters.minDistanceFromCenter.toFloat(), 0f),
            (LUNAR_ECLIPSE_UMBRA_RADIUS - parameters.umbralConeRadius).toFloat()
        )
        val obscuration = Geometry.getIntersectionArea(umbra, moon) / moon.area()
        val visibleStart = if (upAtStart) start else nextEvent(start, zone, location, true) ?: start
        val visibleEnd = if (upAtEnd) end else nextEvent(start, zone, location, false) ?: end
        if (visibleStart >= visibleEnd || visibleStart !in start..end || visibleEnd !in start..end) return null
        val maximum = (start + (end - start) / 2).coerceIn(visibleStart, visibleEnd)
        return LunarEclipseResult(visibleStart, visibleEnd, magnitude.toFloat(), obscuration, maximum)
    }

    private fun nextEvent(start: Instant, zone: TimeZone, location: Coordinate, rise: Boolean): Instant? {
        val today = MoonCalculationModel.events(start, zone, location, true, true)
        val event = if (rise) today.rise else today.set
        if (event != null && event > start) return event
        val tomorrow = MoonCalculationModel.events(
            start.plus(1, DateTimeUnit.DAY, zone), zone, location, true, true
        )
        return (if (rise) tomorrow.rise else tomorrow.set)?.takeIf { it > start }
    }

    private companion object {
        const val MOON_DIAMETER = 0.545
    }
}
