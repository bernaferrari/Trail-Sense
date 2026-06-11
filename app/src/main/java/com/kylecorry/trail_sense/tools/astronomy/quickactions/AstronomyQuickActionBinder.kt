package com.kylecorry.trail_sense.tools.astronomy.quickactions

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences

class AstronomyQuickActionBinder(
    private val host: TrailSenseDestinationHost,
    private val leftButton: ComposeQuickActionButtonView,
    private val rightButton: ComposeQuickActionButtonView,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            leftButton,
            host
        )
        val right = factory.create(
            prefs.rightButton,
            rightButton,
            host
        )
        left.bind(host.lifecycleOwner)
        right.bind(host.lifecycleOwner)
    }

}
