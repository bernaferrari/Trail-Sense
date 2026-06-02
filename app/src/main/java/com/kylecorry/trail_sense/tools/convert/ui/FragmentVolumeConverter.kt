package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import com.kylecorry.sol.units.Volume
import com.kylecorry.sol.units.VolumeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class FragmentVolumeConverter(private val context: Context) :
    SimpleConvertFragment<VolumeUnits>(VolumeUnits.Liters, VolumeUnits.USGallons) {

    private val formatService by lazy { FormatService.getInstance(context) }

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
            VolumeUnits.Liters -> context.getString(R.string.liters)
            VolumeUnits.Milliliter -> context.getString(R.string.milliliters)
            VolumeUnits.USCups -> context.getString(R.string.us_cups)
            VolumeUnits.USPints -> context.getString(R.string.us_pints)
            VolumeUnits.USQuarts -> context.getString(R.string.us_quarts)
            VolumeUnits.USOunces -> context.getString(R.string.us_ounces_volume)
            VolumeUnits.USGallons -> context.getString(R.string.us_gallons)
            VolumeUnits.ImperialCups -> context.getString(R.string.imperial_cups)
            VolumeUnits.ImperialPints -> context.getString(R.string.imperial_pints)
            VolumeUnits.ImperialQuarts -> context.getString(R.string.imperial_quarts)
            VolumeUnits.ImperialOunces -> context.getString(R.string.imperial_ounces_volume)
            VolumeUnits.ImperialGallons -> context.getString(R.string.imperial_gallons)
            VolumeUnits.USTeaspoons -> context.getString(R.string.us_teaspoons)
            VolumeUnits.USTablespoons -> context.getString(R.string.us_tablespoons)
            VolumeUnits.ImperialTeaspoons -> context.getString(R.string.imperial_teaspoons)
            VolumeUnits.ImperialTablespoons -> context.getString(R.string.imperial_tablespoons)
        }
    }

    override fun convert(amount: Float, from: VolumeUnits, to: VolumeUnits): String {
        val converted = Volume.from(amount.absoluteValue, from).convertTo(to)
        return formatService.formatVolume(converted, 4, false)
    }

}
