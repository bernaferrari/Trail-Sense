package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.platform.PlatformContext
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PowerSaverDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: PlatformContext): List<ToolDiagnosticResult> {
        val prefs = getAppService<UserPreferences>()

        return if (prefs.isLowPowerModeOn) {
            listOf(
                ToolDiagnosticResult(
                    "power-saving-mode",
                    ToolDiagnosticSeverity.Warning,
                    getAppService<AppStrings>().get(Res.string.pref_low_power_mode_title),
                    getAppService<AppStrings>().get(Res.string.on),
                    getAppService<AppStrings>().get(Res.string.power_saving_mode_resolution),
                    ToolDiagnosticAction.navigate(
                        Route.PowerSettings,
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
