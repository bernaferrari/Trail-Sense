package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class CreateMapGroupCommand(
    private val mapService: MapService
) : CoroutineCommand<Long?> {
    override suspend fun execute(value: Long?) {
        val name = onMain {
            CoroutinePickers.text(
                AppHost.current(),
                getAppService<AppStrings>().get(Res.string.name),
                hint = getAppService<AppStrings>().get(Res.string.name)
            )
        } ?: return
        mapService.add(MapGroup(0, name, value))
    }
}
