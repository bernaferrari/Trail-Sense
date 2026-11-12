package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class DistanceConverterPage :
    SimpleConverterPage<DistanceUnits>(DistanceUnits.Meters, DistanceUnits.Feet) {

    private val formatService by lazy { FormatService.getInstance() }

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
            DistanceUnits.Meters -> getAppService<AppStrings>().get(Res.string.unit_meters)
            DistanceUnits.Kilometers -> getAppService<AppStrings>().get(Res.string.unit_kilometers)
            DistanceUnits.Feet -> getAppService<AppStrings>().get(Res.string.unit_feet)
            DistanceUnits.Miles -> getAppService<AppStrings>().get(Res.string.unit_miles)
            DistanceUnits.NauticalMiles -> getAppService<AppStrings>().get(Res.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> getAppService<AppStrings>().get(Res.string.unit_centimeters)
            DistanceUnits.Inches -> getAppService<AppStrings>().get(Res.string.unit_inches)
            DistanceUnits.Yards -> getAppService<AppStrings>().get(Res.string.unit_yards)
            DistanceUnits.Millimeters -> getAppService<AppStrings>().get(Res.string.unit_millimeters)
            DistanceUnits.Caliber -> getAppService<AppStrings>().get(Res.string.unit_caliber)
        }
    }

    override fun convertValue(amount: Float, from: DistanceUnits, to: DistanceUnits): Float {
        return Distance.from(amount.absoluteValue, from).convertTo(to).value
    }

    override fun format(amount: Float, unit: DistanceUnits): String {
        return formatService.formatDistance(Distance.from(amount, unit), 4, false)
    }

}
