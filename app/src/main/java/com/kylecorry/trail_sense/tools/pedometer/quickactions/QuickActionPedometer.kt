package com.kylecorry.trail_sense.tools.pedometer.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.luna.topics.generic.ITopic
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

class QuickActionPedometer(btn: QuickActionButtonView, fragment: TrailSenseDestinationHost) :
    TopicQuickAction(btn, fragment, hideWhenUnavailable = false) {

    private val pedometer = PedometerSubsystem.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.steps)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.fragmentToolPedometer)
        return true
    }

    override fun onClick() {
        super.onClick()
        when (pedometer.state.getOrNull()) {
            FeatureState.On -> pedometer.disable()
            FeatureState.Off -> startStepCounter()
            else -> {
                if (pedometer.isDisabledDueToPermissions()) {
                    startStepCounter()
                }
            }
        }
    }

    private fun startStepCounter() {
        fragment.requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                pedometer.enable()
            } else {
                pedometer.disable()
                fragment.alertNoActivityRecognitionPermission()
            }
        }
    }

    override val state: ITopic<FeatureState> = pedometer.state.replay()

}
