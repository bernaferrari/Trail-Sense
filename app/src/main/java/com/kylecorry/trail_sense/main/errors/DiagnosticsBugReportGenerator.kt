package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.BugReportGenerator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class DiagnosticsBugReportGenerator(private val context: Context) : BugReportGenerator {
    override fun generate(error: Throwable): String {
        val diagnostics = Tools.getTools(context)
            .flatMap { it.diagnostics }
            .distinctBy { it.id }


        val codes = diagnostics.flatMap { it.scanner.quickScan(context) }.toSet().sortedBy { it.id }

        return "Diagnostics: ${codes.joinToString(", ") { it.id }}"
    }
}
