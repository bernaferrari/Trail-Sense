package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class DeleteMapCommand(
    private val mapService: MapService
) : CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                AppHost.current(),
                getAppService<AppStrings>().get(Res.string.delete),
                if (!value.isGroup) value.name else getAppService<AppStrings>().get(
                    Res.string.delete_map_group_message,
                    value.name
                )
            )
        }

        if (!shouldDelete) {
            return
        }

        mapService.delete(value)
    }
}
