package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapExportService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.ExportMapCommand

class PhotoMapExportService(private val host: TrailSenseDestinationHost) {
    private val uriPicker = IntentUriPicker(
        host.intentResultRetriever,
        host.requireContext()
    )
    private val loading = AlertLoadingIndicator(
        host.requireContext(),
        host.getString(R.string.exporting_map)
    )
    private val exporter = MapExportService(
        host.requireContext(),
        uriPicker,
        ExternalUriService(host.requireContext())
    )

    private val command = ExportMapCommand(exporter, loading)

    fun export(map: PhotoMap) {
        host.inBackground(BackgroundMinimumState.Created) {
            val success = command.execute(map)
            if (success) {
                host.toast(host.getString(R.string.map_exported))
            } else {
                host.toast(host.getString(R.string.map_export_error))
            }
        }
    }

}
