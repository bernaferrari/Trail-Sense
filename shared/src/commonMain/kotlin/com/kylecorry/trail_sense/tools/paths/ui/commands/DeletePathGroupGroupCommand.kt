package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup

class DeletePathGroupGroupCommand(
    private val pathService: IPathService = getAppService<IPathService>()
) : ISuspendPathGroupCommand {

    override suspend fun execute(group: PathGroup) {
        val cancelled = onMain {
            CoroutineAlerts.dialog(
                AppHost.current(),
                getAppService<AppStrings>().get(Res.string.delete),
                getAppService<AppStrings>().get(Res.string.delete_path_group_message, group.name)
            )
        }

        if (cancelled) {
            return
        }

        onIO {
            pathService.deleteGroup(group)
        }
    }
}
