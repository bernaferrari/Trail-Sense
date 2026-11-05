package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.content.Context
import androidx.core.net.toUri
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

class CreateMapFromCameraCommand(
    private val context: Context,
    private val effects: ScreenEffects,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): IMap? {
        val uri = onMain { takePhoto() }?.toUri() ?: return null
        return onIO {
            onMain {
                loadingIndicator.show()
            }
            try {
                val map = CreateMapFromUriCommand(
                    context,
                    uri,
                    loadingIndicator
                ).execute()
                DeleteTempFilesCommand(context).execute()
                map
            } finally {
                onMain {
                    loadingIndicator.hide()
                }
            }
        }
    }

    private suspend fun takePhoto(): String? {
        if (!effects.requestPermission(AppPermission.Camera)) {
            return null
        }
        val zeroShutterLag = UserPreferences(context).camera.useZeroShutterLag
        return effects.capturePhoto(zeroShutterLag = zeroShutterLag)
    }
}
