package com.kylecorry.trail_sense.tools.navigation.quickactions

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences

class NavigationQuickActionBinder(
    private val fragment: TrailSenseDestinationHost,
    private val leftButton: ComposeQuickActionButtonView,
    private val rightButton: ComposeQuickActionButtonView,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            leftButton,
            fragment
        )
        val right = factory.create(
            prefs.rightButton,
            rightButton,
            fragment
        )
        left.bind(fragment.lifecycleOwner)
        right.bind(fragment.lifecycleOwner)
    }

}
