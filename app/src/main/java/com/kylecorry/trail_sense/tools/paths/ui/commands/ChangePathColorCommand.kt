package com.kylecorry.trail_sense.tools.paths.ui.commands

import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ChangePathColorCommand(
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService
) {

    fun execute(path: Path, color: AppColor) {
        lifecycleOwner.inBackground {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(style = path.style.copy(color = color.color)))
            }
        }
    }
}
