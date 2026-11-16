package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class LastTideSelectionStrategy(
    private val prefs: ITidePreferences,
    private val clearLastTideAfterUse: Boolean = false
) :
    ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? =
        onIO {
            val lastTide = prefs.lastTide
            if (clearLastTideAfterUse) {
                prefs.lastTide = null
            }
            tides.firstOrNull { tide -> tide.id == lastTide }
        }
}
