package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.trail_sense.shared.time.JavaTime.toJava

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.shared.time.JavaTime.toZonedDateTime
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.time.Instant

class CurrentTidePhaseCommand(
    private val tideService: TideService,
    private val timeProvider: ZonedDateTimeProvider = SystemZonedDateTimeProvider()
) {

    suspend fun execute(table: TideTable, time: Instant? = null): Float? = onDefault {
        val now = time?.toZonedDateTime() ?: timeProvider.getTime().toJava()
        tideService.getPhase(table, now)
    }

}
