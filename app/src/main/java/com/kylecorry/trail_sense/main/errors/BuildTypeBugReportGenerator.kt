package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.exceptions.IBugReportGenerator
import com.kylecorry.trail_sense.shared.debugging.getBuildType

class BuildTypeBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        return "Build type: ${getBuildType()}"
    }
}
