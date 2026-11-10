package com.kylecorry.trail_sense.tools.diagnostics.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.DiagnosticsLogExportService
import kotlinx.coroutines.CancellationException
import java.time.Instant

/** Asks where to save the diagnostics logs, then writes them there as a zip. */
class DownloadDiagnosticsLogsCommand(
    private val context: Context,
    private val resultRetriever: IntentResultRetriever,
    private val effects: ScreenEffects
) {

    suspend fun execute() {
        val destination = IntentUriPicker(resultRetriever, context).create(
            "trail-sense-logs-${Instant.now().epochSecond}.zip",
            "application/zip"
        ) ?: return

        val indicator = AlertLoadingIndicator(
            context,
            context.getString(R.string.exporting)
        )
        try {
            indicator.show()
            DiagnosticsLogExportService(context).export(destination)
            effects.showMessage(context.getString(R.string.exported))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            effects.showMessage(context.getString(R.string.export_failed))
        } finally {
            indicator.hide()
        }
    }
}
