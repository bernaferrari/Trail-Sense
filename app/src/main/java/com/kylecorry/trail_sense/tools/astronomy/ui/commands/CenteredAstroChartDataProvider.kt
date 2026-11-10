package com.kylecorry.trail_sense.tools.astronomy.ui.commands

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IsTodaySpecification
import kotlin.time.Instant

class CenteredAstroChartDataProvider(
    private val astronomy: AstronomyService = AstronomyService(),
    private val isToday: Specification<Instant> = IsTodaySpecification()
) : IAstroChartDataProvider {

    override fun get(location: Coordinate, time: Instant): AstroChartData {
        if (isToday.isSatisfiedBy(time)) {
            val moon = astronomy.getCenteredMoonAltitudes(
                location,
                time
            )
            val sun = astronomy.getCenteredSunAltitudes(
                location,
                time
            )
            return AstroChartData(sun, moon)
        }
        return DailyAstroChartDataProvider().get(location, time)
    }

}
