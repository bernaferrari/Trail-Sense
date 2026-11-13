package com.kylecorry.trail_sense.tools.diagnostics.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects

/** Diagnostics on Android, where the logs are exported through the activity's file picker. */
@Composable
fun DiagnosticsAndroidToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    DiagnosticsToolScreen(
        exportLogs = {
            // The activity implements the Activity-only IntentResultRetriever the file picker needs.
            DownloadDiagnosticsLogsCommand(context, context as IntentResultRetriever, effects).execute()
        }
    )
}
