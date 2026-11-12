package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * The app's navigation: a Navigation 3 back stack of [Route]s that [TrailSenseNavDisplay] draws.
 *
 * Plain Kotlin and Compose state only (no Android types): screens call [navigate] and [popBackStack]; the activity
 * saves the stack with [saveState] and the system back gesture pops it.
 *
 * A route is on the stack at most once, whatever the operation: Navigation 3 keys each entry's saved state and
 * ViewModels by its route, so two equal routes would share (and fight over) them.
 */
class TrailSenseNavigator {

    fun interface OnRouteChangedListener {
        fun onRouteChanged(navigator: TrailSenseNavigator, route: Route)
    }

    /** The back stack itself, as Compose state; the last route is on screen. */
    val backStack = mutableStateListOf<Route>()

    /** Whether the latest change switched tabs (a new root) rather than going forward or back. */
    var lastChangeWasTabSwitch by mutableStateOf(false)
        private set

    /** The first route on the stack: the tab the app opened on. */
    val startRoute: Route?
        get() = backStack.firstOrNull()

    val currentRoute: Route?
        get() = backStack.lastOrNull()

    private val listeners = mutableListOf<OnRouteChangedListener>()

    /** Starts over with only [route] on the stack. */
    fun setStartRoute(route: Route) {
        backStack.clear()
        backStack.add(route)
        lastChangeWasTabSwitch = true
        notifyChanged()
    }

    /**
     * Opens [route] on top of the current screen, as upstream's Fragment navigation did: back always returns to the
     * screen it was opened from. A route already on the stack is moved to the top rather than stacked a second time,
     * because each Navigation 3 entry needs a unique key (its saved state is keyed by the route).
     */
    fun navigate(route: Route) {
        pushUnique(route)
        lastChangeWasTabSwitch = false
        notifyChanged()
    }

    /**
     * Switches to a tab: everything above the start route is dropped, then [route] goes on top of it (or replaces it
     * when the tab is the start route itself).
     */
    fun switchTab(route: Route) {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        when {
            backStack.isEmpty() -> backStack.add(route)
            backStack.first()::class == route::class -> backStack[0] = route
            else -> backStack.add(route)
        }
        lastChangeWasTabSwitch = true
        notifyChanged()
    }

    /**
     * Replaces the screen on top with [route], so back skips it (a form that becomes the thing it created). When
     * [route] is already further down the stack, that entry moves to the top instead of being stacked a second time.
     */
    fun replace(route: Route) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        pushUnique(route)
        lastChangeWasTabSwitch = false
        notifyChanged()
    }

    /** Pops the top screen; false when only the first screen is left (the activity handles that back press). */
    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        lastChangeWasTabSwitch = false
        notifyChanged()
        return true
    }

    fun navigateUp(): Boolean = popBackStack()

    fun isOnTop(type: KClass<out Route>): Boolean = currentRoute?.let { type.isInstance(it) } == true

    fun addOnRouteChangedListener(listener: OnRouteChangedListener) {
        listeners += listener
        currentRoute?.let { listener.onRouteChanged(this, it) }
    }

    fun removeOnRouteChangedListener(listener: OnRouteChangedListener) {
        listeners -= listener
    }

    /** The stack as JSON, for the activity's saved state. */
    fun saveState(): String = json.encodeToString(stackSerializer, backStack.toList())

    /** Restores a stack saved by [saveState]; false when there was nothing usable to restore. */
    fun restoreState(state: String?): Boolean {
        state ?: return false
        val stack = runCatching { json.decodeFromString(stackSerializer, state) }.getOrNull()
        if (stack.isNullOrEmpty()) return false
        backStack.clear()
        // A stack saved by an older build may hold a route twice: keep each route's last (most recent) place
        backStack.addAll(stack.asReversed().distinct().asReversed())
        lastChangeWasTabSwitch = true
        notifyChanged()
        return true
    }

    /** Puts [route] on top, moving it there if it is already on the stack. */
    private fun pushUnique(route: Route) {
        val existing = backStack.indexOf(route)
        if (existing == backStack.lastIndex && existing >= 0) return
        if (existing >= 0) backStack.removeAt(existing)
        backStack.add(route)
    }

    private fun notifyChanged() {
        val route = currentRoute ?: return
        listeners.toList().forEach { it.onRouteChanged(this, route) }
    }

    companion object {
        private val stackSerializer = ListSerializer(Route.serializer())
        private val json = Json { ignoreUnknownKeys = true }

        /** A route as a string, for intent extras (notifications, widgets, shortcuts). */
        fun encode(route: Route): String = json.encodeToString(Route.serializer(), route)

        fun decode(value: String?): Route? =
            value?.let { runCatching { json.decodeFromString(Route.serializer(), it) }.getOrNull() }
    }
}
