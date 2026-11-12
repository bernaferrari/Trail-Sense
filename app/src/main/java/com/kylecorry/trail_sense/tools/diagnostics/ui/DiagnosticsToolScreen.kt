package com.kylecorry.trail_sense.tools.diagnostics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import kotlinx.coroutines.launch

/** Diagnostics: what is wrong with the sensors and permissions, each with its fix, and the logs to export. */
@Composable
fun DiagnosticsToolScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    DiagnosticsScreen(
        onShowSensorDetails = {
            navigator.navigate(Route.SensorDetails)
        },
        onShowDiagnostic = {
            DiagnosticAlerter().alert(it) { action ->
                action.action(context, navigator)
            }
        },
        onExportLogs = {
            scope.launch {
                // The activity implements the Activity-only IntentResultRetriever the file picker needs.
                DownloadDiagnosticsLogsCommand(context, context as IntentResultRetriever, effects).execute()
            }
        }
    )
}
