package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigateWithAnimation

data class ToolDiagnosticAction(
    val name: String,
    val action: (fragment: Fragment) -> Unit
) {
    companion object {
        fun navigate(
            @IdRes to: Int,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                it.findNavController().navigateWithAnimation(to)
            }
        }

        fun command(
            command: Command,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                command.execute()
            }
        }

        fun intent(
            intent: Intent,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                it.startActivity(intent)
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
