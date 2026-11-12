package com.kylecorry.trail_sense.tools.paths.ui.commands

import kotlinx.coroutines.IO
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.ui.getPointStyleNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePointStyleCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        val names = getPointStyleNames()
        val styles = PathPointColoringStyle.entries
        Pickers.item(AppHost.current(), getAppService<AppStrings>().get(Res.string.point_style), styles.map { names.getValue(it) },
            defaultSelectedIndex = styles.indexOf(path.style.point)
        ) {
            if (it != null) {
                val pointStyle = styles.getOrElse(it) { PathPointColoringStyle.None }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(style = path.style.copy(point = pointStyle)))
                    }
                }
            }
        }
    }
}
