package com.kylecorry.trail_sense.tools.astronomy.ui.commands

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.time.toLocalDate
import kotlin.time.Instant

class DailyAstroChartDataProvider(private val astronomy: AstronomyService = AstronomyService()) :
    IAstroChartDataProvider {
    override fun get(location: Coordinate, time: Instant): AstroChartData {
        val sun = astronomy.getSunAltitudes(location, time.toLocalDate())
        val moon = astronomy.getMoonAltitudes(location, time.toLocalDate())
        return AstroChartData(sun, moon)
    }
}
