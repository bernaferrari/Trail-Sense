package com.kylecorry.trail_sense.main.errors

import com.kylecorry.andromeda.exceptions.BugReportGenerator
import com.kylecorry.trail_sense.shared.debugging.getBuildType

class BuildTypeBugReportGenerator : BugReportGenerator {
    override fun generate(error: Throwable): String {
        return "Build type: ${getBuildType()}"
    }
}
