package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.platform.PlatformContext

data class ToolDiagnosticAction(
    val name: String,
    val action: (context: PlatformContext, appNavigator: TrailSenseNavigator) -> Unit
) {
    companion object {
        fun navigate(
            to: Route,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) { _, appNavigator ->
                appNavigator.navigate(to)
            }
        }

        fun command(
            command: Command,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) { _, _ ->
                command.execute()
            }
        }
    }
}
