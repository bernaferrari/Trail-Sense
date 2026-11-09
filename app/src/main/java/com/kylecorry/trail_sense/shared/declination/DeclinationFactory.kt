package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences

class DeclinationFactory {

    fun getDeclinationStrategy(
        prefs: IDeclinationPreferences,
        gps: GpsSensor? = null
    ): IDeclinationStrategy {
        return if (gps == null || !prefs.useAutoDeclination) {
            OverrideDeclinationStrategy(prefs)
        } else {
            GPSDeclinationStrategy(gps)
        }
    }

}
