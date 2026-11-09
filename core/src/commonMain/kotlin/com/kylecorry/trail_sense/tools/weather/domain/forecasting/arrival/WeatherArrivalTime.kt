package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import kotlin.time.Duration
import kotlin.time.Instant

data class WeatherArrivalTime(val time: Instant, val isExact: Boolean) {
    fun toRelative(now: Instant): RelativeArrivalTime {
        val timeUntil = (time - now)
        return when {
            timeUntil <= 10.minutes -> RelativeArrivalTime.Now
            timeUntil <= 1.hours -> RelativeArrivalTime.VerySoon
            timeUntil <= 2.hours -> RelativeArrivalTime.Soon
            else -> RelativeArrivalTime.Later
        }
    }

    companion object {
        fun fromRelative(now: Instant, approximate: RelativeArrivalTime): WeatherArrivalTime {
            val duration = when (approximate) {
                RelativeArrivalTime.Now -> Duration.ZERO
                RelativeArrivalTime.VerySoon -> 1.hours
                RelativeArrivalTime.Soon -> 2.hours
                RelativeArrivalTime.Later -> 8.hours
            }
            return WeatherArrivalTime(now.plus(duration), false)
        }
    }
}
