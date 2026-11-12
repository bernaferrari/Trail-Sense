package com.kylecorry.trail_sense.tools.whitenoise.quickactions

import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import kotlinx.coroutines.launch

class QuickActionWhiteNoise(btn: QuickActionButtonView, host: AndroidDestinationHost) :
    QuickActionButton(btn, host) {

    private val stateListener: (EventData) -> Unit = {
        host.lifecycleOwner.lifecycleScope.launch {
            setState(isOn())
        }
    }

    private fun isOn(): Boolean {
        return WhiteNoiseService.isRunning
    }

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolWhiteNoise)
    }

    override fun onClick() {
        super.onClick()
        if (isOn()) {
            WhiteNoiseService.stop(context)
        } else {
            WhiteNoiseService.play(context)
        }
    }

    override fun onResume() {
        super.onResume()
        setState(isOn())
        ToolBroadcasts.subscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onPause() {
        super.onPause()
        ToolBroadcasts.unsubscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

}
