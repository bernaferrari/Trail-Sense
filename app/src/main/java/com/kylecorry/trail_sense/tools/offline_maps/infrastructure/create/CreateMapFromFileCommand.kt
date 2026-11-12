package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import com.kylecorry.luna.result.errorOrNull
import com.kylecorry.trail_sense.shared.io.UriPickerError
import android.content.Context
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): CreateMapResult = onIO {
        val picked = uriPicker.open(
            listOf("image/*", "application/pdf", "application/octet-stream"),
            requirePersistentAccess = true
        )
        val uri = picked.getOrNull()
            ?: return@onIO CreateMapResult.Failed(
                if (picked.errorOrNull() == UriPickerError.AccessDenied) {
                    CreateOfflineMapError.AccessDenied
                } else {
                    CreateOfflineMapError.Cancelled
                }
            )
        CreateMapFromUriCommand(context, uri, loadingIndicator).execute()
    }
}
