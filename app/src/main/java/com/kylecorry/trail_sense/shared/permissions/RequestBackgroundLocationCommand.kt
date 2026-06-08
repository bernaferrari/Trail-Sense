package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RequestBackgroundLocationCommand(
    context: Context,
    alerter: IAlerter,
    flag: Flag = PreferencesFlag(
        PreferencesSubsystem.getInstance(context).preferences,
        SHOWN_KEY
    ),
    isRequired: Specification<Context> = IsBackgroundLocationRequired()
) : RequestOptionalPermissionCommand(
    context,
    flag,
    alerter,
    isRequired
) {
    companion object {
        private const val SHOWN_KEY = "cache_background_location_requested"
    }
}
