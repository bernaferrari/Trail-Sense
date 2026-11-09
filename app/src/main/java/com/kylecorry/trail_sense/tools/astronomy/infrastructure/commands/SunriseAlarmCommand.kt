package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import com.kylecorry.trail_sense.shared.time.today

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.days
import kotlin.time.Clock
import kotlin.time.Instant

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.notifications.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.core.app.NotificationCompat
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.andromeda_temp.notify.Notify
import com.kylecorry.trail_sense.shared.andromeda_temp.permissions.Permissions
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import kotlin.time.Duration

class SunriseAlarmCommand(private val context: Context) : CoroutineCommand {

    private val location by lazy { LocationSubsystem.getInstance(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val astronomyService = AstronomyService()

    // The window prior the alert time that the alert can be sent
    private val alertWindow = 20.minutes

    // The window after the alert time that the alert can be sent
    private val alertWindowAfter = 5.minutes

    override suspend fun execute() = onDefault {
        val now = Clock.System.now()

        if (location.location == Coordinate.zero) {
            setAlarm((now + 1.days), "no location available")
            return@onDefault
        }

        val alertDuration = userPrefs.astronomy.sunriseAlertMinutesBefore.minutes
        val suntimesMode = userPrefs.astronomy.sunTimesMode

        val todaySunrise = astronomyService.getTodaySunTimes(location.location, suntimesMode).rise

        val tomorrowSunrise =
            astronomyService.getTomorrowSunTimes(location.location, suntimesMode).rise

        if (todaySunrise != null) {
            when {
                isPastSunrise(todaySunrise) -> {
                    // Missed the sunrise, schedule the alarm for tomorrow
                    setAlarm(
                        tomorrowSunrise?.minus(alertDuration)
                            ?: (todaySunrise + 1.days),
                        "past today's sunrise at $todaySunrise"
                    )
                }

                withinAlertWindow(todaySunrise, alertDuration) -> {
                    // Send alert, schedule alarm for tomorrow's sunrise or else at some point tomorrow
                    sendNotification(todaySunrise)
                    setAlarm(
                        tomorrowSunrise?.minus(alertDuration)
                            ?: (todaySunrise + 1.days),
                        "within alert window for today's sunrise at $todaySunrise"
                    )
                }

                else -> { // Before the alert window
                    // Schedule alarm for sunrise
                    setAlarm(
                        todaySunrise.minus(alertDuration),
                        "before alert window for today's sunrise at $todaySunrise"
                    )
                }
            }
        } else {
            // There isn't a sunrise today, schedule it for tomorrow
            setAlarm(tomorrowSunrise?.minus(alertDuration) ?: (now + 1.days), "no sunrise today")
        }
    }

    private fun isPastSunrise(sunrise: Instant): Boolean {
        return Clock.System.now() > sunrise + alertWindowAfter
    }

    private fun withinAlertWindow(sunrise: Instant, alertDuration: Duration): Boolean {
        val alertTime = sunrise.minus(alertDuration)
        val minAlertTime = alertTime.minus(alertWindow)
        val alertRange = Range(minAlertTime, sunrise.plus(alertWindowAfter))
        return alertRange.contains(Clock.System.now())
    }

    private fun sendNotification(sunrise: Instant) {

        val lastSentDate = userPrefs.astronomy.sunriseAlertLastSent
        if (today() == lastSentDate) {
            getAppService<Logger>().info(TAG, "Sunrise alert already sent today")
            return
        }

        userPrefs.astronomy.setSunriseAlertLastSentDate(today())

        val formatService = FormatService.getInstance(context)
        val formattedTime = formatService.formatTime(sunrise, false)

        val openIntent = NavigationUtils.pendingIntent(context, Route.Astronomy)

        val useAlarm = userPrefs.astronomy.useAlarmForSunriseAlert
        val notificationChannel = if (useAlarm) {
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNRISE_ALARM
        } else {
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNRISE_ALERT
        }
        val notification = Notify.alert(
            context,
            notificationChannel,
            context.getString(R.string.sunrise_alert_notification_title),
            context.getString(
                R.string.sunrise_alert_notification_text,
                formattedTime
            ),
            TrailIcons.SunriseNotification,
            intent = openIntent,
            autoCancel = true,
            isAlarm = useAlarm,
            category = NotificationCompat.CATEGORY_REMINDER
        )

        DependencyRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)
        getAppService<Logger>().info(TAG, "Sunrise alert sent")
    }

    private fun setAlarm(time: Instant, reason: String) {
        val scheduler = SunriseAlarmReceiver.scheduler(context)
        scheduler.cancel()
        val instant = time
        scheduler.once(instant)
        getAppService<Logger>().info(
            TAG,
            "Scheduled next run at $instant (exact: ${Permissions.canScheduleExactAlarms(context)}): $reason"
        )
    }

    companion object {
        const val TAG = "SunriseAlarmCommand"
        const val NOTIFICATION_ID = 1232
        const val NOTIFICATION_CHANNEL_ID = "Sunrise alert"
    }

}
