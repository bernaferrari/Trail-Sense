package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class WaterBoilTimerPreferences(context: Context) : PreferenceRepo(context) {

    var useAlarm by BooleanPreference(
        cache,
        PreferenceKeys.waterBoilTimerUseAlarm,
        false
    )

}
