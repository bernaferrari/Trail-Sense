package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletePointCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        Alerts.dialog(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.delete_waypoint_prompt)
        ) { cancelled ->
            if (!cancelled) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.deleteWaypoint(point)
                    }
                }
            }
        }
    }
}
