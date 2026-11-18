package com.kylecorry.trail_sense.tools.astronomy.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.tools.astronomy.AstronomyBroadcasts
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Schedules the next four weeks of almanac alerts when the host can deliver local notifications. */
internal class ScheduledSunAlerts(
    override val name: String,
    private val isSunrise: Boolean,
    private val preferences: AstronomyPreferences,
    private val location: suspend () -> Coordinate?,
    private val replaceNotifications: suspend (String, String, List<Instant>) -> Boolean
) : ToolService {
    override val id = if (isSunrise) AstronomyBroadcasts.SERVICE_SUNRISE_ALERTS else AstronomyBroadcasts.SERVICE_SUNSET_ALERTS
    private val astronomy = AstronomyService()
    private val changes = Mutex()
    override fun getFrequency() = 1.days
    override fun isRunning() = isEnabled()
    override fun isEnabled() = if (isSunrise) preferences.sendSunriseAlerts else preferences.sendSunsetAlerts
    override fun isBlocked() = false

    override suspend fun enable() = changes.withLock {
        setEnabled(schedule())
    }

    override suspend fun disable() = changes.withLock {
        setEnabled(false)
        replaceNotifications(id, name, emptyList())
        Unit
    }

    override suspend fun restart() = changes.withLock {
        if (isEnabled()) schedule()
        Unit
    }

    override suspend fun stop() = changes.withLock {
        replaceNotifications(id, name, emptyList())
        Unit
    }

    private suspend fun schedule(): Boolean {
        val coordinate = location() ?: return false
        val now = Clock.System.now()
        val leadTime = if (isSunrise) preferences.sunriseAlertMinutesBefore.minutes else preferences.sunsetAlertMinutesBefore.minutes
        val times = withContext(Dispatchers.Default) {
            val mode = preferences.sunTimesMode
            var after = now + leadTime
            buildList {
                repeat(28) {
                    val event = if (isSunrise) astronomy.getNextSunrise(coordinate, mode, after)
                    else astronomy.getNextSunset(coordinate, mode, after)
                    if (event == null || event - now > 29.days) return@buildList
                    add(event - leadTime)
                    after = event + 1.seconds
                }
            }
        }
        return replaceNotifications(id, name, times)
    }

    private fun setEnabled(value: Boolean) {
        val broadcast = if (isSunrise) {
            preferences.sendSunriseAlerts = value
            if (value) AstronomyBroadcasts.SUNRISE_ALERTS_ENABLED else AstronomyBroadcasts.SUNRISE_ALERTS_DISABLED
        } else {
            preferences.sendSunsetAlerts = value
            if (value) AstronomyBroadcasts.SUNSET_ALERTS_ENABLED else AstronomyBroadcasts.SUNSET_ALERTS_DISABLED
        }
        ToolBroadcasts.broadcast(broadcast)
    }
}
