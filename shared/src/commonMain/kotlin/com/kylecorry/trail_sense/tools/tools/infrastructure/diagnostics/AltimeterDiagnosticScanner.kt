package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.platform.PlatformContext
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AltimeterDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: PlatformContext): List<ToolDiagnosticResult> {
        val prefs = getAppService<UserPreferences>()

        return if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            listOf(
                ToolDiagnosticResult(
                    "altitude-overridden",
                    ToolDiagnosticSeverity.Warning,
                    getAppService<AppStrings>().get(Res.string.elevation),
                    getAppService<AppStrings>().get(Res.string.overridden),
                    resolution = getAppService<AppStrings>().get(Res.string.elevation_override_resolution),
                    action = ToolDiagnosticAction.navigate(
                        Route.CalibrateAltimeter,
                        getAppService<AppStrings>().get(Res.string.settings)
                    )
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: PlatformContext): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}
