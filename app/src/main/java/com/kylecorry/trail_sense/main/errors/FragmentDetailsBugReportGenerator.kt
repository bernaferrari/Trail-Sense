package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.trail_sense.main.MainActivity

class FragmentDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val destination = if (context is MainActivity) {
            context.getActiveDestinationHost()?.javaClass?.simpleName
        } else {
            null
        }
        return "Fragment: ${destination ?: MainActivity.lastKnownDestinationHost ?: "Unknown"}"
    }
}
