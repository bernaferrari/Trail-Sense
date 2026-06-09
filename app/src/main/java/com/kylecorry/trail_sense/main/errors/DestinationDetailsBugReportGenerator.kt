package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.trail_sense.main.MainActivity

class DestinationDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val destination = if (context is MainActivity) {
            context.getActiveDestinationHost()?.javaClass?.simpleName
        } else {
            null
        }
        return "Destination: ${destination ?: MainActivity.lastKnownDestinationHost ?: "Unknown"}"
    }
}
