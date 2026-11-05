package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command

data class ToolDiagnosticAction(
    val name: String,
    val action: (context: Context, appNavigator: TrailSenseNavigator) -> Unit
) {
    companion object {
        fun navigate(
            @IdRes to: Int,
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

        fun intent(
            intent: Intent,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) { context, _ ->
                context.startActivity(intent)
            }
        }

        fun permissions(context: Context): ToolDiagnosticAction {
            return intent(Intents.appSettings(context), context.getString(R.string.settings))
        }

        fun notification(context: Context, channel: String?): ToolDiagnosticAction {
            return intent(
                Intents.notificationSettings(context, channel),
                context.getString(R.string.settings)
            )
        }
    }
}
