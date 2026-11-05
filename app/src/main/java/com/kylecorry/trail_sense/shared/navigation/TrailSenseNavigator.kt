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
 * Plain Kotlin and Compose state only (no Android types), so it could live in common code: screens call
 * [navigate] and [popBackStack]; the activity saves the stack with [saveState] and the system back gesture pops it.
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
     * Opens [route] on top of the current screen. A route already on the stack is returned to (everything above it
     * is dropped) instead of being stacked twice, so back never loops and each entry keeps a unique key.
     */
    fun navigate(route: Route) {
        val existing = backStack.indexOf(route)
        if (existing >= 0) {
            while (backStack.size > existing + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            backStack.add(route)
        }
        lastChangeWasTabSwitch = false
        notifyChanged()
    }

    /**
     * Switches to a tab: everything above the start route is dropped, then [route] goes on top of it (or replaces it
     * when the tab is the start route itself).
     */
    fun switchTab(route: Route) {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (backStack.isEmpty() || backStack.first()::class == route::class) {
            if (backStack.isEmpty()) backStack.add(route) else backStack[0] = route
        } else {
            backStack.add(route)
        }
        lastChangeWasTabSwitch = true
        notifyChanged()
    }

    /** Replaces the screen on top with [route], so back skips it (a form that becomes the thing it created). */
    fun replace(route: Route) {
        if (backStack.isEmpty()) {
            backStack.add(route)
        } else {
            backStack[backStack.lastIndex] = route
        }
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
        backStack.addAll(stack)
        lastChangeWasTabSwitch = true
        notifyChanged()
        return true
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
