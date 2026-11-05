package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.getPointStyleNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChangePointStyleCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        val names = getPointStyleNames(context)
        val styles = PathPointColoringStyle.entries
        Pickers.item(
            context, context.getString(R.string.point_style), styles.map { names.getValue(it) },
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
