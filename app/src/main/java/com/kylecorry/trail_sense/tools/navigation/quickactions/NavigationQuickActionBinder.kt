package com.kylecorry.trail_sense.tools.navigation.quickactions

import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment

class NavigationQuickActionBinder(
    private val fragment: NavigatorFragment,
    private val navigationTitle: Toolbar,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(prefs.leftButton, navigationTitle.leftButton, fragment)
        val right = factory.create(prefs.rightButton, navigationTitle.rightButton, fragment)
        left.bind(fragment)
        right.bind(fragment)
    }

}
