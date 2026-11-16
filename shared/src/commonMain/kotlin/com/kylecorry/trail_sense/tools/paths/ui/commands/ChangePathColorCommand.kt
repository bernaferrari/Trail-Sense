package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePathColorCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService
) {

    fun execute(path: Path, color: AppColor) {
        scope.launch {
            withContext(ioDispatcher) {
                pathService.addPath(path.copy(style = path.style.copy(color = color.color)))
            }
        }
    }
}
