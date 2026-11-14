package com.kylecorry.andromeda.views.chart.label

import com.kylecorry.andromeda.core.math.DecimalFormatter

public class NumberChartLabelFormatter(private val places: Int = 0, private val strict: Boolean = true) :
    ChartLabelFormatter {
    override fun format(value: Float): String {
        return DecimalFormatter.format(value, places, strict)
    }
}