package com.kylecorry.andromeda.core.time

import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class SystemZonedDateTimeProvider : ZonedDateTimeProvider {
    override fun getTime(): ZonedDateTime = ZonedDateTime(Clock.System.now(), TimeZone.currentSystemDefault())
}
