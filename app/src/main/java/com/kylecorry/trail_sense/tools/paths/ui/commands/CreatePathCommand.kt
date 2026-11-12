package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.dialogs.text
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences

class CreatePathCommand(
    private val context: Context,
    private val pathService: IPathService = getAppService<IPathService>(),
    private val pathPreferences: IPathPreferences = UserPreferences(context).paths
) {

    suspend fun execute(parentId: Long?): Long? {
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.path),
                hint = context.getString(R.string.name)
            )
        } ?: return null

        return onIO {
            pathService.addPath(
                Path(
                    0,
                    name,
                    pathPreferences.defaultPathStyle,
                    PathMetadata.empty,
                    parentId = parentId
                )
            )
        }
    }
}
