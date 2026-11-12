package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.FormatService

class TimeConverterPage :
    SimpleConverterPage<TimeUnits>(TimeUnits.Minutes, TimeUnits.Hours) {

    private val formatService by lazy { FormatService.getInstance() }

    override val units = listOf(
        TimeUnits.Seconds,
        TimeUnits.Minutes,
        TimeUnits.Hours,
        TimeUnits.Days
    )

    override fun getUnitName(unit: TimeUnits): String {
        return when (unit) {
            TimeUnits.Seconds -> getAppService<AppStrings>().get(Res.string.seconds)
            TimeUnits.Minutes -> getAppService<AppStrings>().get(Res.string.minutes)
            TimeUnits.Hours -> getAppService<AppStrings>().get(Res.string.hours)
            TimeUnits.Days -> getAppService<AppStrings>().get(Res.string.days)
            else -> ""
        }
    }

    override fun convertValue(amount: Float, from: TimeUnits, to: TimeUnits): Float {
        val seconds = amount * from.seconds
        return (seconds / to.seconds).toFloat()
    }

    override fun format(amount: Float, unit: TimeUnits): String {
        return formatService.formatTime(amount, unit, 4, false)
    }

}
