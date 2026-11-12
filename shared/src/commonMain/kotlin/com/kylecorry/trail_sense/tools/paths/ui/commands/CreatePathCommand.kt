package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences

class CreatePathCommand(
    private val host: AppHost,
    private val pathService: IPathService = getAppService<IPathService>(),
    private val pathPreferences: IPathPreferences = getAppService<UserPreferences>().paths
) {

    suspend fun execute(parentId: Long?): Long? {
        val strings = getAppService<AppStrings>()
        val name = onMain {
            CoroutinePickers.text(
                host,
                strings.get(Res.string.path),
                hint = strings.get(Res.string.name)
            )
        } ?: return null

        return onIO {
            pathService.addPath(
                Path(
                    0,
                    name,
                    pathPreferences.defaultPathStyle,
                    PathMetadata.empty,
                    parentId = parentId
                )
            )
        }
    }
}
