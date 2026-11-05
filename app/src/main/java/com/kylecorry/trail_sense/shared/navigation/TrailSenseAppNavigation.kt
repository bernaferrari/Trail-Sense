package com.kylecorry.trail_sense.shared.navigation

import android.os.Bundle

class TrailSenseAppNavigation(private val navigator: TrailSenseNavigator) : IAppNavigation {
    override fun navigate(actionId: Int, params: Bundle) {
        navigator.navigate(actionId, params)
    }
}
