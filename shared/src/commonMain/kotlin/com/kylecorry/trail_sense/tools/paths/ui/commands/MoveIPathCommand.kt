package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathPickers

class MoveIPathCommand(
    private val host: AppHost,
    private val pathService: IPathService = getAppService<IPathService>()
) {

    suspend fun execute(path: IPath): PathGroup? {

        val result = PathPickers.pickGroup(
            host,
            null,
            getAppService<AppStrings>().get(Res.string.move),
            initialGroup = path.parentId,
            filter = { it.filter { path !is PathGroup || path.id != it.id } }
        )

        val newGroup = result.second?.id

        if (result.first || newGroup == path.parentId) {
            return onIO {
                pathService.getGroup(path.parentId)
            }
        }

        return onIO {
            if (path is PathGroup) {
                pathService.addGroup(path.copy(parentId = newGroup))
            } else {
                pathService.addPath((path as Path).copy(parentId = newGroup))
            }

            result.second
        }
    }

}
