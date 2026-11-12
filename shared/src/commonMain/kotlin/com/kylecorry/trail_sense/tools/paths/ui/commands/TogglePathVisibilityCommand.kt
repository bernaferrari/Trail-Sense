package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TogglePathVisibilityCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        scope.launch {
            onIO {
                pathService.addPath(path.copy(style = path.style.copy(visible = !path.style.visible)))
            }
        }
    }
}
