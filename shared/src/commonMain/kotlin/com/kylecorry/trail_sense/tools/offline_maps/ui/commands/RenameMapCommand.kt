package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class RenameMapCommand(private val service: MapService) : CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val newName = CoroutinePickers.text(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.name),
            default = value.name
        ) ?: return
        when (value) {
            is MapGroup -> service.add(value.copy(name = newName))
            is PhotoMap -> service.add(value.copy(name = newName))
            is VectorMap -> service.add(value.copy(name = newName))
        }
    }
}
