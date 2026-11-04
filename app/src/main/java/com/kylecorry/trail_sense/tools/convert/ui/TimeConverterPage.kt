package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class TimeConverterPage(private val context: Context) :
    SimpleConverterPage<TimeUnits>(TimeUnits.Minutes, TimeUnits.Hours) {

    private val formatService by lazy { FormatService.getInstance(context) }

    override val units = listOf(
        TimeUnits.Seconds,
        TimeUnits.Minutes,
        TimeUnits.Hours,
        TimeUnits.Days
    )

    override fun getUnitName(unit: TimeUnits): String {
        return when (unit) {
            TimeUnits.Seconds -> context.getString(R.string.seconds)
            TimeUnits.Minutes -> context.getString(R.string.minutes)
            TimeUnits.Hours -> context.getString(R.string.hours)
            TimeUnits.Days -> context.getString(R.string.days)
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
