package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.luna.specifications.Specification
import kotlinx.datetime.LocalTime

class CanSendDailyForecast(private val startTime: LocalTime) : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val end = LocalTime.fromSecondOfDay((startTime.toSecondOfDay() + 3 * 3600) % (24 * 3600))
        return value >= startTime && value <= end
    }
}
