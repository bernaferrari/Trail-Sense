package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Package
import com.kylecorry.trail_sense.shared.andromeda_temp.exceptions.IBugReportGenerator
class PackageNameBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        return "Package: ${Package.getPackageName(context)}"
    }
}
