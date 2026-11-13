package com.kylecorry.trail_sense.tools.paths.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import androidx.lifecycle.lifecycleScope

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand

class QuickActionBacktrack(btn: QuickActionButtonView, host: AndroidDestinationHost) :
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
        host.navigator.navigate(Route.Paths)
        return true
    }

    override fun onClick() {
        super.onClick()
        val command = ToggleBacktrackCommand(host as ScreenEffects, host.lifecycleOwner.lifecycleScope)
        command.execute()
    }
}
