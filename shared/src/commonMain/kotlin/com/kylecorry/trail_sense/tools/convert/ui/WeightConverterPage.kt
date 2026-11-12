package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class WeightConverterPage :
    SimpleConverterPage<WeightUnits>(WeightUnits.Kilograms, WeightUnits.Pounds) {

    private val formatService by lazy { FormatService.getInstance() }

    override val units = WeightUnits.values().toList()

    override fun getUnitName(unit: WeightUnits): String {
        return formatService.getWeightUnitName(unit)
    }

    override fun convertValue(amount: Float, from: WeightUnits, to: WeightUnits): Float {
        return Weight.from(amount.absoluteValue, from).convertTo(to).value
    }

    override fun format(amount: Float, unit: WeightUnits): String {
        return formatService.formatWeight(Weight.from(amount, unit), 4, false)
    }

}
