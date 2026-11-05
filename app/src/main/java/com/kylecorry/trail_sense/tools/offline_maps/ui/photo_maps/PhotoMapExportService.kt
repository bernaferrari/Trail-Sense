package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapExportService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.ExportMapCommand

class PhotoMapExportService(
    private val context: Context,
    intentResultRetriever: IntentResultRetriever,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner
) {
    private val uriPicker = IntentUriPicker(
        intentResultRetriever,
        context
    )
    private val loading = AlertLoadingIndicator(
        context,
        context.getString(R.string.exporting_map)
    )
    private val exporter = MapExportService(
        context,
        uriPicker,
        ExternalUriService(context)
    )

    private val command = ExportMapCommand(exporter, loading)

    fun export(map: PhotoMap) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
            val success = command.execute(map)
            if (success) {
                effects.showMessage(context.getString(R.string.map_exported))
            } else {
                effects.showMessage(context.getString(R.string.map_export_error))
            }
        }
    }

}
