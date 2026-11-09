package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.exceptions.BugReportGenerator

class PackageNameBugReportGenerator(private val context: Context) : BugReportGenerator {
    override fun generate(error: Throwable): String {
        return "Package: ${Package.getPackageName(context)}"
    }
}
