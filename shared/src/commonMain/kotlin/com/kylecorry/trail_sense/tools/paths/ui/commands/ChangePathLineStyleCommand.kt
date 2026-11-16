package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.ui.getLineStyleNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePathLineStyleCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        val names = getLineStyleNames()
        val styles = LineStyle.entries
        Pickers.item(AppHost.current(), getAppService<AppStrings>().get(Res.string.line_style), styles.map { names.getValue(it) },
            defaultSelectedIndex = styles.indexOf(path.style.line)
        ) {
            if (it != null) {
                val line = styles.getOrElse(it) { LineStyle.Dotted }
                scope.launch {
                    withContext(ioDispatcher) {
                        pathService.addPath(path.copy(style = path.style.copy(line = line)))
                    }
                }
            }
        }
    }
}
