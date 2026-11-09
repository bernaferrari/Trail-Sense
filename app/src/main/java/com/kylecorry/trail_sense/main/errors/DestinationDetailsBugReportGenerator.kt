package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.BugReportGenerator
import com.kylecorry.trail_sense.main.MainActivity

class DestinationDetailsBugReportGenerator(private val context: Context) : BugReportGenerator {
    override fun generate(error: Throwable): String {
        val destination = if (context is MainActivity) {
            context.getActiveDestinationHost()?.javaClass?.simpleName
        } else {
            null
        }
        return "Destination: ${destination ?: MainActivity.lastKnownDestinationHost ?: "Unknown"}"
    }
}
