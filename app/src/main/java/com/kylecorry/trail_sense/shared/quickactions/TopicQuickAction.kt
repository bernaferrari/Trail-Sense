package com.kylecorry.trail_sense.shared.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.QuickActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** A quick action that follows a feature's [state] while it is shown: on, off, or (optionally hidden) unavailable. */
abstract class TopicQuickAction(
    btn: QuickActionButtonView,
    host: AndroidDestinationHost,
    private val hideWhenUnavailable: Boolean = false
) :
    QuickActionButton(btn, host) {

    abstract val state: Flow<FeatureState>

    private var stateJob: Job? = null

    override fun onResume() {
        super.onResume()
        stateJob?.cancel()
        stateJob = host.lifecycleOwner.lifecycleScope.launch {
            state.distinctUntilChanged().collect(::onStateChange)
        }
    }

    override fun onPause() {
        super.onPause()
        stateJob?.cancel()
        stateJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

    private fun onStateChange(state: FeatureState) {
        button.isVisible = !hideWhenUnavailable || state != FeatureState.Unavailable
        setState(state == FeatureState.On)
    }
}
