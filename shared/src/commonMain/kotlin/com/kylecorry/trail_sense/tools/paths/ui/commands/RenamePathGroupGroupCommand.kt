package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup

class RenamePathGroupGroupCommand(
    private val pathService: IPathService = getAppService<IPathService>()
) : ISuspendPathGroupCommand {

    override suspend fun execute(group: PathGroup) {
        val newName = onMain {
            CoroutinePickers.text(
                AppHost.current(),
                getAppService<AppStrings>().get(Res.string.rename),
                default = group.name,
                hint = getAppService<AppStrings>().get(Res.string.name)
            )
        } ?: return

        onIO {
            pathService.addGroup(group.copy(name = newName))
        }
    }
}
