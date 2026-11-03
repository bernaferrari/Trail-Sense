package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.DiagnosticsLogExportService
import kotlinx.coroutines.CancellationException
import java.time.Instant

class DownloadDiagnosticsLogsCommand(private val host: TrailSenseDestinationHost) {

    fun execute() {
        host.inBackground(BackgroundMinimumState.Created) {
            val destination = IntentUriPicker(host.intentResultRetriever, host.requireContext()).create(
                "trail-sense-logs-${Instant.now().epochSecond}.zip",
                "application/zip"
            ) ?: return@inBackground

            val indicator = AlertLoadingIndicator(
                host.requireContext(),
                host.getString(R.string.exporting)
            )
            try {
                indicator.show()
                DiagnosticsLogExportService(host.requireContext()).export(destination)
                host.toast(host.getString(R.string.exported))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                host.toast(host.getString(R.string.export_failed))
            } finally {
                indicator.hide()
            }
        }
    }
}
