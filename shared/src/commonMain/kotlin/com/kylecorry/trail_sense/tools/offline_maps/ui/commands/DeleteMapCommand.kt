package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class DeleteMapCommand(
    private val mapService: MapService,
    private val host: AppHost,
    private val strings: AppStrings
) {
    suspend fun execute(value: IMap): Boolean {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                host,
                strings.get(Res.string.delete),
                if (!value.isGroup) value.name else strings.get(
                    Res.string.delete_map_group_message,
                    value.name
                )
            )
        }

        if (!shouldDelete) {
            return false
        }

        mapService.delete(value)
        return true
    }
}
