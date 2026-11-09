package com.kylecorry.trail_sense.receivers

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Package
class StartOnBootController(private val context: Context) {

    private val component = "com.kylecorry.trail_sense.receivers.BootReceiver"

    fun enable() {
        Package.setComponentEnabled(
            context,
            component,
            true
        )
    }

    fun disable() {
        Package.setComponentEnabled(
            context,
            component,
            false
        )
    }

}
