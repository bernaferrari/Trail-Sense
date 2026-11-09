package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import kotlin.time.Clock

import com.kylecorry.trail_sense.shared.time.today

import com.kylecorry.trail_sense.shared.time.plusDays

import kotlin.time.Duration.Companion.days

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.notifications.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.andromeda_temp.notify.Notify
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import kotlin.time.Duration

class MeteorShowerAlertCommand(private val context: Context) : Command<Coordinate> {
    override fun execute(value: Coordinate) {
        val prefs = UserPreferences(context)
        val shouldSend = prefs.astronomy.sendMeteorShowerAlerts

        if (!shouldSend) {
            return
        }

        val astronomyService = AstronomyService()
        val today = today()
        val todayShower = astronomyService.getMeteorShower(value, today)
        val tomorrowShower = astronomyService.getMeteorShower(value, today.plusDays(1))

        val shower = listOfNotNull(todayShower, tomorrowShower).firstOrNull {
            val timeUntilPeak = it.peak - kotlin.time.Clock.System.now()
            timeUntilPeak >= Duration.ZERO && timeUntilPeak <= 1.days
        }

        if (shower == null) {
            return
        }

        val notification = Notify.status(
            context,
            AstronomyAlertCommand.NOTIFICATION_CHANNEL,
            context.getString(R.string.meteor_shower),
            getShowerDescription(context, shower),
            TrailIcons.Astronomy,
            group = AstronomyAlertCommand.NOTIFICATION_GROUP_ASTRONOMY_ALERTS,
            intent = NavigationUtils.pendingIntent(context, Route.Astronomy),
            autoCancel = true
        )

        DependencyRegistry.get<NotificationSubsystem>().send(732094, notification)
    }

    private fun getShowerDescription(context: Context, shower: MeteorShowerPeak): String {
        val formatService = FormatService.getInstance(context)

        val peak = formatService.formatRelativeDateTime(shower.peak, includeSeconds = false)

        val rate = context.getString(R.string.meteors_per_hour, shower.shower.rate)

        return "$peak\n$rate"
    }

}
