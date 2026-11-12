package com.kylecorry.trail_sense.tools.astronomy.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class SunsetAlertsToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = AstronomyToolRegistration.SERVICE_SUNSET_ALERTS

    override val name: String = context.getString(R.string.sunset_alerts)

    override fun getFrequency(): Duration {
        return 1.days
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.astronomy.sendSunsetAlerts
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        prefs.astronomy.sendSunsetAlerts = true
        ToolBroadcasts.broadcast(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED)
        ToolBroadcasts.broadcast(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_STATE_CHANGED)
        restart()
    }

    override suspend fun disable() {
        prefs.astronomy.sendSunsetAlerts = false
        ToolBroadcasts.broadcast(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED)
        ToolBroadcasts.broadcast(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_STATE_CHANGED)
        stop()
    }

    override suspend fun restart() {
        // Always starts - it short circuits if it doesn't need to run
        SunsetAlarmReceiver.start(context)
    }

    override suspend fun stop() {
        SunsetAlarmReceiver.scheduler(context).cancel()
    }
}
