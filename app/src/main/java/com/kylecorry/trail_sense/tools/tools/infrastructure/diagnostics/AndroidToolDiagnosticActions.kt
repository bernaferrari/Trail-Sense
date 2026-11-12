package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R

fun ToolDiagnosticAction.Companion.intent(
    intent: Intent,
    title: String
): ToolDiagnosticAction {
    return ToolDiagnosticAction(title) { context, _ ->
        context.startActivity(intent)
    }
}

fun ToolDiagnosticAction.Companion.permissions(context: Context): ToolDiagnosticAction {
    return intent(Intents.appSettings(context), context.getString(R.string.settings))
}

fun ToolDiagnosticAction.Companion.notification(context: Context, channel: String?): ToolDiagnosticAction {
    return intent(
        Intents.notificationSettings(context, channel),
        context.getString(R.string.settings)
    )
}
