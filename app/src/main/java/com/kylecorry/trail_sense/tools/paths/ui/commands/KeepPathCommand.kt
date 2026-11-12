package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeepPathCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        scope.launch {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(temporary = false))
            }
        }
    }
}
