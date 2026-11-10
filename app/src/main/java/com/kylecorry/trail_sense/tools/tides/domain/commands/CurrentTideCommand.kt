package com.kylecorry.trail_sense.tools.tides.domain.commands


import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData

class CurrentTideCommand(
    private val tideService: TideService,
    private val timeProvider: ZonedDateTimeProvider = SystemZonedDateTimeProvider()
) {

    suspend fun execute(table: TideTable): CurrentTideData = onDefault {
        val now = timeProvider.getTime()
        val level = tideService.getWaterLevel(table, now.instant, now.timeZone)
        val isRising = tideService.isRising(table, now.instant, now.timeZone)
        val type = tideService.getCurrentTide(table, now.instant, now.timeZone)
        val withinTable = tideService.isWithinTideTable(table, now.instant)
        CurrentTideData(if (withinTable) level else null, type, isRising)
    }

}
