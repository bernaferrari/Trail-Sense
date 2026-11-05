package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DeletePointCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        Alerts.dialog(
            context,
            context.getString(R.string.delete_waypoint_prompt)
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
