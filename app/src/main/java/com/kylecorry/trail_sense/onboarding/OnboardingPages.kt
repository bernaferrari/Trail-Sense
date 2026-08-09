package com.kylecorry.trail_sense.onboarding

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.MissingSensorAlert
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.sensors.SensorService

object OnboardingPages {

    fun getPages(context: Context): List<OnboardingPage> {
        return listOfNotNull(
            OnboardingPage(
                context.getString(R.string.app_disclaimer_message_title),
                context.getString(R.string.disclaimer_message_content),
                TrailSenseIcons.Filled.Gavel,
                context.getString(R.string.i_agree)
            ),
            if (SensorService(context).hasCompass()) {
                null
            } else {
                OnboardingPage(
                    MissingSensorAlert.getMissingSensorTitle(
                        context,
                        context.getString(R.string.pref_compass_sensor_title)
                    ),
                    MissingSensorAlert.getMissingSensorMessage(
                        context,
                        context.getString(R.string.pref_compass_sensor_title)
                    ),
                    TrailSenseIcons.Filled.SensorsOff
                )
            }
        )
    }

}
