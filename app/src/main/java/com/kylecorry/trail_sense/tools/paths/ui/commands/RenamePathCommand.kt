package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.text
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePathCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.text(
            context,
            context.getString(R.string.rename),
            default = path.name,
            hint = context.getString(R.string.name)
        ) {
            if (it != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(name = it.ifBlank { null }))
                    }
                }

            }
        }
    }
}
