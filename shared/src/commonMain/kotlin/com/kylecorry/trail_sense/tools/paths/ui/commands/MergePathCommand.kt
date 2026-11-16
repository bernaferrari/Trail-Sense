package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathPickers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MergePathCommand(
    private val host: AppHost,
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    private val strings = getAppService<AppStrings>()

    override fun execute(path: Path) {
        scope.launch {
            val other = PathPickers.pickPath(
                host,
                strings.get(Res.string.append_onto),
                scope = this,
                filter = { it.filter { it !is Path || it.id != path.id } }
            ) ?: return@launch

            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(host, strings.get(Res.string.merging))
            }
            withContext(ioDispatcher) {
                pathService.mergePaths(other.id, path.id)
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
            }

        }
    }
}
