package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.sol.units.Volume
import com.kylecorry.sol.units.VolumeUnits
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class VolumeConverterPage :
    SimpleConverterPage<VolumeUnits>(VolumeUnits.Liters, VolumeUnits.USGallons) {

    private val formatService by lazy { FormatService.getInstance() }

    override val units = listOf(
        VolumeUnits.Milliliter,
        VolumeUnits.Liters,
        VolumeUnits.USTeaspoons,
        VolumeUnits.USTablespoons,
        VolumeUnits.USOunces,
        VolumeUnits.USCups,
        VolumeUnits.USPints,
        VolumeUnits.USQuarts,
        VolumeUnits.USGallons,
        VolumeUnits.ImperialTeaspoons,
        VolumeUnits.ImperialTablespoons,
        VolumeUnits.ImperialOunces,
        VolumeUnits.ImperialCups,
        VolumeUnits.ImperialPints,
        VolumeUnits.ImperialQuarts,
        VolumeUnits.ImperialGallons
    )

    override fun getUnitName(unit: VolumeUnits): String {
        return when (unit) {
            VolumeUnits.Liters -> getAppService<AppStrings>().get(Res.string.liters)
            VolumeUnits.Milliliter -> getAppService<AppStrings>().get(Res.string.milliliters)
            VolumeUnits.USCups -> getAppService<AppStrings>().get(Res.string.us_cups)
            VolumeUnits.USPints -> getAppService<AppStrings>().get(Res.string.us_pints)
            VolumeUnits.USQuarts -> getAppService<AppStrings>().get(Res.string.us_quarts)
            VolumeUnits.USOunces -> getAppService<AppStrings>().get(Res.string.us_ounces_volume)
            VolumeUnits.USGallons -> getAppService<AppStrings>().get(Res.string.us_gallons)
            VolumeUnits.ImperialCups -> getAppService<AppStrings>().get(Res.string.imperial_cups)
            VolumeUnits.ImperialPints -> getAppService<AppStrings>().get(Res.string.imperial_pints)
            VolumeUnits.ImperialQuarts -> getAppService<AppStrings>().get(Res.string.imperial_quarts)
            VolumeUnits.ImperialOunces -> getAppService<AppStrings>().get(Res.string.imperial_ounces_volume)
            VolumeUnits.ImperialGallons -> getAppService<AppStrings>().get(Res.string.imperial_gallons)
            VolumeUnits.USTeaspoons -> getAppService<AppStrings>().get(Res.string.us_teaspoons)
            VolumeUnits.USTablespoons -> getAppService<AppStrings>().get(Res.string.us_tablespoons)
            VolumeUnits.ImperialTeaspoons -> getAppService<AppStrings>().get(Res.string.imperial_teaspoons)
            VolumeUnits.ImperialTablespoons -> getAppService<AppStrings>().get(Res.string.imperial_tablespoons)
        }
    }

    override fun convertValue(amount: Float, from: VolumeUnits, to: VolumeUnits): Float {
        return Volume.from(amount.absoluteValue, from).convertTo(to).value
    }

    override fun format(amount: Float, unit: VolumeUnits): String {
        return formatService.formatVolume(Volume.from(amount, unit), 4, false)
    }

}
