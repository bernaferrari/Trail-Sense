package com.kylecorry.trail_sense.tools.astronomy.ui.commands

import com.kylecorry.sol.units.Coordinate
import kotlin.time.Instant

interface IAstroChartDataProvider {
    fun get(location: Coordinate, time: Instant): AstroChartData
}
