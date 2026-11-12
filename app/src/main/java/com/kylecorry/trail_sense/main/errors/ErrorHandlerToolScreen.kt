package com.kylecorry.trail_sense.main.errors

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.copy
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/** The page shown after a tool crashed ([Route.ErrorHandler]): report it, reopen the tool, or restart. */
@Composable
fun ErrorHandlerToolScreen(host: AndroidDestinationHost, route: Route.ErrorHandler) {
    val toolId = route.toolId
    val errorDetails = route.error
    val appNavigator = host.navigator
    ErrorHandlerScreen(
        toolId = toolId,
        onEmail = {
            host.context.startActivity(
                Intents.email(
                    host.context.getString(R.string.email),
                    "Error in ${host.context.getString(R.string.app_name)}",
                    errorDetails
                )
            )
        },
        onCopyError = {
            Clipboard.copy(
                host.context,
                errorDetails,
                host.context.getString(R.string.copied_to_clipboard_toast)
            )
        },
        onViewDetails = {
            Alerts.dialog(
                host.context,
                host.context.getString(R.string.error_occurred),
                errorDetails,
                cancelText = null
            )
        },
        onReopenTool = {
            Tools.getTool(host.context, toolId)?.let { appNavigator.navigate(it.route) }
        },
        onRestartApp = {
            CurrentApp.restart(host.context)
        },
        onOpenSettings = {
            appNavigator.navigate(Route.Settings)
        }
    )
}
