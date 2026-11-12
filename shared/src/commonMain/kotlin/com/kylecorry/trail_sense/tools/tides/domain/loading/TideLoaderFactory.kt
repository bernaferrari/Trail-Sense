package com.kylecorry.trail_sense.tools.tides.domain.loading

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.EstimateTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.FallbackTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo

class TideLoaderFactory {

    fun getTideLoader(
        useLastTide: Boolean = true,
        locationOverride: Coordinate? = null
    ): ITideLoader {
        val prefs = getAppService<UserPreferences>().tides
        val location = getAppService<ILocationSubsystem>()
        val strategy = if (prefs.showNearestTide && useLastTide) {
            FallbackTideSelectionStrategy(
                LastTideSelectionStrategy(prefs, true),
                NearestTideSelectionStrategy { locationOverride ?: location.location }
            )
        } else if (prefs.showNearestTide || !useLastTide) {
            NearestTideSelectionStrategy { locationOverride ?: location.location }
        } else {
            LastTideSelectionStrategy(prefs, false)
        }

        return TideLoaderImpl(
            getAppService<ITideTableRepo>(),
            FallbackTideSelectionStrategy(
                strategy,
                EstimateTideSelectionStrategy(),
                LastTideSelectionStrategy(prefs, false),
                DefaultTideSelectionStrategy()
            )
        )
    }

}
