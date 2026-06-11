package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

class CreateMapFromCameraCommand(
    private val host: TrailSenseDestinationHost,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): IMap? = onIO {
        val uri = CustomUiUtils.takePhoto(host) ?: return@onIO null
        onMain {
            loadingIndicator.show()
        }
        try {
            val map = CreateMapFromUriCommand(
                host.requireContext(),
                uri,
                loadingIndicator
            ).execute()
            DeleteTempFilesCommand(host.requireContext()).execute()
            map
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }
}
