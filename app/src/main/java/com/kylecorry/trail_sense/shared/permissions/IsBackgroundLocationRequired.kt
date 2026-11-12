package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService

class IsBackgroundLocationRequired : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val sensorService = getAppService<SensorService>()
        val hasForegroundLocation = sensorService.hasLocationPermission()
        val hasBackgroundLocation = sensorService.hasLocationPermission(true)

        if (!hasForegroundLocation || hasBackgroundLocation) {
            return false
        }

        val prefs = UserPreferences(value)
        return prefs.astronomy.sendSunsetAlerts
    }
}
