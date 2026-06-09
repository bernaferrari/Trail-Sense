package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import kotlinx.coroutines.flow.StateFlow

@Navigator.Name(TrailSenseComposeNavigator.NAME)
class TrailSenseComposeNavigator : Navigator<TrailSenseComposeNavigator.Destination>(NAME) {

    var isReady by mutableStateOf(false)
        private set

    val backStack: StateFlow<List<NavBackStackEntry>>
        get() = state.backStack

    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        isReady = true
    }

    override fun createDestination(): Destination {
        return Destination(this)
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { state.push(it) }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }

    class Destination(navigator: TrailSenseComposeNavigator) : NavDestination(navigator)

    companion object {
        const val NAME = "trail_sense_compose"
    }
}
