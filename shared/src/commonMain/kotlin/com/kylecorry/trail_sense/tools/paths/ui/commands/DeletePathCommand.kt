package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletePathCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        Alerts.dialog(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.delete_path),
            getAppService<AppStrings>().plural(
                Res.plurals.waypoints_to_be_deleted,
                path.metadata.waypoints,
                path.metadata.waypoints
            )
        ) { cancelled ->
            if (!cancelled) {
                scope.launch {
                    withContext(ioDispatcher) {
                        pathService.deletePath(path)
                    }
                }
            }
        }
    }
}
