package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapPickers
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class MoveMapCommand(private val host: AppHost, private val service: MapService) :
    CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val strings = getAppService<AppStrings>()
        val results = MapPickers.pickGroup(
            host,
            null,
            strings.get(Res.string.move),
            initialGroup = value.parentId
        ) {
            it.filter {
                if (value is MapGroup) {
                    it.id != value.id
                } else {
                    true
                }
            }
        }

        if (results.first) {
            return
        }

        when (value) {
            is MapGroup -> service.add(value.copy(parentId = results.second?.id))
            is PhotoMap -> service.add(value.copy(parentId = results.second?.id))
            is VectorMap -> service.add(value.copy(parentId = results.second?.id))
        }

        val groupName = results.second?.name ?: strings.get(Res.string.no_group)

        onMain {
            Alerts.snackbar(host, strings.get(Res.string.moved_to, groupName))
        }
    }
}
