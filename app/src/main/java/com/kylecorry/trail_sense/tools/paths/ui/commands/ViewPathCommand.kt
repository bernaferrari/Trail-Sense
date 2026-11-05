package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.os.Bundle
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.Path

class ViewPathCommand(
    private val appNavigator: TrailSenseNavigator
) : IPathCommand {

    override fun execute(path: Path) {
        execute(path.id)
    }

    fun execute(id: Long) {
        appNavigator.navigate(R.id.action_backtrack_to_path, Bundle().apply {
            putLong("path_id", id)
        })
    }
}
