package com.kylecorry.trail_sense.shared.navigation

class TrailSenseAppNavigation(private val navigator: TrailSenseNavigator) : IAppNavigation {
    override fun navigate(route: Route) {
        navigator.navigate(route)
    }
}
