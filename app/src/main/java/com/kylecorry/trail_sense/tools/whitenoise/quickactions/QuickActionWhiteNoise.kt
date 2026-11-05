package com.kylecorry.trail_sense.tools.whitenoise.quickactions

import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.os.Bundle
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService

class QuickActionWhiteNoise(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    private val stateListener: (Bundle) -> Unit = {
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
        Tools.subscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

}
