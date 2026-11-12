package com.kylecorry.trail_sense.tools.astronomy.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences

/** Binds the astronomy title's two quick actions; [quickActionHost] is only for the shared QuickActionFactory. */
class AstronomyQuickActionBinder(
    private val quickActionHost: AndroidDestinationHost,
    private val lifecycleOwner: LifecycleOwner,
    private val leftButton: ComposeQuickActionButtonView,
    private val rightButton: ComposeQuickActionButtonView,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            leftButton,
            quickActionHost
        )
        val right = factory.create(
            prefs.rightButton,
            rightButton,
            quickActionHost
        )
        left.bind(lifecycleOwner)
        right.bind(lifecycleOwner)
    }

}

/** Binds the astronomy title's quick actions for the shared astronomy screen. */
fun AndroidDestinationHost.bindAstronomyQuickActions(
    left: ComposeQuickActionButtonView,
    right: ComposeQuickActionButtonView,
    lifecycleOwner: LifecycleOwner
) {
    AstronomyQuickActionBinder(this, lifecycleOwner, left, right, getAppService<UserPreferences>().astronomy).bind()
}
