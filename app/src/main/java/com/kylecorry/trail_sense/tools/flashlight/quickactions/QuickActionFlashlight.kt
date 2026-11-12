package com.kylecorry.trail_sense.tools.flashlight.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuickActionFlashlight(btn: QuickActionButtonView, host: AndroidDestinationHost) :
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
        host.navigator.navigate(Route.Flashlight)
        return true
    }

    override val state: Flow<FeatureState> = flashlight.mode.map {
        if (flashlight.isAvailable()) {
            when (it) {
                FlashlightMode.Torch -> FeatureState.On
                else -> FeatureState.Off
            }
        } else {
            FeatureState.Unavailable
        }
    }

}
