package com.kylecorry.trail_sense.tools.paths.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand

class QuickActionBacktrack(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    ToolServiceQuickAction(
        btn,
        host,
        PathsToolRegistration.SERVICE_BACKTRACK,
        PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
        hideWhenUnavailable = false
    ) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolBacktrack)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigateWithAnimation(R.id.fragmentBacktrack)
        return true
    }

    override fun onClick() {
        super.onClick()
        val command = ToggleBacktrackCommand(host)
        command.execute()
    }
}
