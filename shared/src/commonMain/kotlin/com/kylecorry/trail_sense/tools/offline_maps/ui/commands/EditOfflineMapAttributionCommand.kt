package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class EditOfflineMapAttributionCommand : CoroutineCommand<VectorMap> {
    private val service = getAppService<MapService>()

    override suspend fun execute(value: VectorMap) {
        val attribution = CoroutinePickers.text(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.attribution),
            hint = getAppService<AppStrings>().get(Res.string.attribution),
            default = value.attribution.orEmpty()
        ) ?: return

        service.add(value.copy(attribution = attribution.trim()))
    }
}
