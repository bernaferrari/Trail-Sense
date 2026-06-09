package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Intent
import com.kylecorry.trail_sense.main.MainActivity

fun interface ToolIntentHandler {
    fun handle(activity: MainActivity, intent: Intent): Boolean
}
