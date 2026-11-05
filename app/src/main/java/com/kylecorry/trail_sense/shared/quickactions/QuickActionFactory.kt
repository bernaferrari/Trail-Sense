package com.kylecorry.trail_sense.shared.quickactions

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionFactory {

    fun create(
        id: Int,
        button: QuickActionButtonView,
        host: TrailSenseDestinationHost
    ): QuickActionButton {
        val quickActions = Tools.getQuickActions(host.context)
        button.contentDescription = "quick_action_$id"
        return quickActions.firstOrNull { it.id == id }?.create?.invoke(button, host)
            ?: QuickActionNone(button, host)
    }

}
