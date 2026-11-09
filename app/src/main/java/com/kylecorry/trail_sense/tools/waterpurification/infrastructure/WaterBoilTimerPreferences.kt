package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class WaterBoilTimerPreferences(context: Context) : PreferenceRepo(context) {

    var useAlarm by BooleanPreference(
        cache,
        getString(R.string.pref_water_boil_timer_use_alarm),
        false
    )

}
