package com.kylecorry.trail_sense.shared.navigation

/** Opens a screen; the seam domain code (such as BeaconNavigator) navigates through, so tests can fake it. */
fun interface IAppNavigation {
    fun navigate(route: Route)
}
