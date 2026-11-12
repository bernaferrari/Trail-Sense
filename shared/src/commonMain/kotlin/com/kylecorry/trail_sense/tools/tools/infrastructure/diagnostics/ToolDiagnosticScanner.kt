package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import com.kylecorry.trail_sense.shared.platform.PlatformContext
import kotlinx.coroutines.flow.Flow

interface ToolDiagnosticScanner {
    fun quickScan(context: PlatformContext): List<ToolDiagnosticResult>
    fun fullScan(context: PlatformContext): Flow<List<ToolDiagnosticResult>>
}
