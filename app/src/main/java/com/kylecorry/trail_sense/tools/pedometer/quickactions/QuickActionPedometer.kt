package com.kylecorry.trail_sense.tools.pedometer.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AppPermission

import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.luna.topics.generic.ITopic
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

class QuickActionPedometer(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    TopicQuickAction(btn, host, hideWhenUnavailable = false) {

    private val pedometer = PedometerSubsystem.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.Steps)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.Pedometer)
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
        host.lifecycleOwner.lifecycleScope.launch {
            if (host.requestPermission(AppPermission.ActivityRecognition)) {
                pedometer.enable()
            } else {
                pedometer.disable()
                host.explainPermissionDenied(AppPermission.ActivityRecognition)
            }
        }
    }

    override val state: ITopic<FeatureState> = pedometer.state.replay()

}
