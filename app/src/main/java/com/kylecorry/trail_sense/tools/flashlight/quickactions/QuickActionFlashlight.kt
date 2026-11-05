package com.kylecorry.trail_sense.tools.flashlight.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.luna.topics.generic.ITopic
import com.kylecorry.luna.topics.generic.distinct
import com.kylecorry.luna.topics.generic.map
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionFlashlight(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    TopicQuickAction(btn, host, hideWhenUnavailable = true) {

    private val flashlight by lazy { FlashlightSubsystem.getInstance(context) }

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.Flashlight)
    }

    override fun onClick() {
        super.onClick()
        flashlight.toggle()
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().openTool(Tools.FLASHLIGHT)
        return true
    }

    override val state: ITopic<FeatureState> = flashlight.mode.map {
        if (flashlight.isAvailable()) {
            when (it) {
                FlashlightMode.Torch -> FeatureState.On
                else -> FeatureState.Off
            }
        } else {
            FeatureState.Unavailable
        }
    }.distinct().replay()

}
