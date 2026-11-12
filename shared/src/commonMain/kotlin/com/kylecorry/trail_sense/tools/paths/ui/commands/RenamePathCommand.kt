package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePathCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.text(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.rename),
            default = path.name,
            hint = getAppService<AppStrings>().get(Res.string.name)
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
