package com.kylecorry.trail_sense.shared.quickactions

import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState

abstract class ToolServiceQuickAction(
    btn: QuickActionButtonView,
    host: AndroidDestinationHost,
    private val serviceId: String,
    private val stateChangeBroadcastId: String,
    private val hideWhenUnavailable: Boolean = false
) : QuickActionButton(btn, host) {

    protected val service by lazy { Tools.getService(context, serviceId) }
    protected val state: FeatureState
        get() = service?.getFeatureState() ?: FeatureState.Unavailable

    override fun onResume() {
        super.onResume()
        updateState()
        ToolBroadcasts.subscribe(stateChangeBroadcastId, this::onStateChange)
    }

    override fun onPause() {
        super.onPause()
        ToolBroadcasts.unsubscribe(stateChangeBroadcastId, this::onStateChange)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

    private fun onStateChange(data: EventData) {
        updateState()
    }

    private fun updateState() {
        button.isVisible = !hideWhenUnavailable || state != FeatureState.Unavailable
        setState(state == FeatureState.On)
    }


}
