package com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SunsetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val shouldSend = UserPreferences(context).astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        val pendingResult = goAsync()

        val command = SunsetAlarmCommand(context.applicationContext)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                command.execute()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        private const val PI_ID = 8309

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).exact(
                SunsetAlarmReceiver::class.java,
                PI_ID
            )
        }

        fun start(context: Context) {
            context.sendBroadcast(Intent(context, SunsetAlarmReceiver::class.java))
        }

        // TODO: Extract this out of the receiver
        /**
         * Enable sunset alerts and, with [effects], request permissions if needed
         */
        suspend fun enable(
            context: Context,
            effects: ScreenEffects?,
            scope: CoroutineScope
        ) {
            enableAstronomyAlert(
                context,
                AstronomyToolRegistration.SERVICE_SUNSET_ALERTS,
                effects,
                scope
            )
        }
    }
}
