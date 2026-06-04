package com.kylecorry.trail_sense.tools.astronomy.quickactions

import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.shared.quickactions.ImageButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyFragment

class AstronomyQuickActionBinder(
    private val fragment: AstronomyFragment,
    private val astronomyTitle: Toolbar,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            ImageButtonQuickActionView(astronomyTitle.leftButton),
            fragment
        )
        val right = factory.create(
            prefs.rightButton,
            ImageButtonQuickActionView(astronomyTitle.rightButton),
            fragment
        )
        left.bind(fragment)
        right.bind(fragment)
    }

}
