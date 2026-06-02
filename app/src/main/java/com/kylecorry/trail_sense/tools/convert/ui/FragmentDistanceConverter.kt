package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class FragmentDistanceConverter(private val context: Context) :
    SimpleConvertFragment<DistanceUnits>(DistanceUnits.Meters, DistanceUnits.Feet) {

    private val formatService by lazy { FormatService.getInstance(context) }

    override val units = listOf(
        DistanceUnits.Millimeters,
        DistanceUnits.Centimeters,
        DistanceUnits.Meters,
        DistanceUnits.Kilometers,
        DistanceUnits.Inches,
        DistanceUnits.Feet,
        DistanceUnits.Yards,
        DistanceUnits.Miles,
        DistanceUnits.NauticalMiles,
    )


    override fun getUnitName(unit: DistanceUnits): String {
        return when (unit) {
            DistanceUnits.Meters -> context.getString(R.string.unit_meters)
            DistanceUnits.Kilometers -> context.getString(R.string.unit_kilometers)
            DistanceUnits.Feet -> context.getString(R.string.unit_feet)
            DistanceUnits.Miles -> context.getString(R.string.unit_miles)
            DistanceUnits.NauticalMiles -> context.getString(R.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> context.getString(R.string.unit_centimeters)
            DistanceUnits.Inches -> context.getString(R.string.unit_inches)
            DistanceUnits.Yards -> context.getString(R.string.unit_yards)
            DistanceUnits.Millimeters -> context.getString(R.string.unit_millimeters)
            DistanceUnits.Caliber -> context.getString(R.string.unit_caliber)
        }
    }

    override fun convert(amount: Float, from: DistanceUnits, to: DistanceUnits): String {
        val converted = Distance.from(amount.absoluteValue, from).convertTo(to)
        return formatService.formatDistance(converted, 4, false)
    }

}
