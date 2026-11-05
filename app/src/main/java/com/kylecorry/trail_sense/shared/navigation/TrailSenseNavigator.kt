package com.kylecorry.trail_sense.shared.navigation

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.BundleCompat
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * One entry on the Navigation 3 back stack: which screen, its arguments, and a key unique to this visit (the same
 * screen twice on the stack is two entries, each with its own saved state).
 */
@Parcelize
data class TrailSenseRoute(
    @IdRes val destinationId: Int,
    val arguments: Bundle,
    val key: String = UUID.randomUUID().toString()
) : Parcelable

/** How a navigation treats the stack: pop back to a screen first, and/or reuse the top entry when it is the same screen. */
data class TrailSenseNavOptions(
    @IdRes val popUpTo: Int? = null,
    val popUpToInclusive: Boolean = false,
    val launchSingleTop: Boolean = false
)

/**
 * The app's navigation: a Navigation 3 back stack of [TrailSenseRoute]s that [TrailSenseNavDisplay] draws. Screens
 * navigate by destination id (or one of the old action ids, resolved through [TrailSenseDestinations]).
 */
class TrailSenseNavigator(
    val context: Context,
    private val destinations: Map<Int, TrailSenseDestination> = TrailSenseDestinations.all
) {

    fun interface OnDestinationChangedListener {
        fun onDestinationChanged(
            navigator: TrailSenseNavigator,
            destination: TrailSenseDestination,
            arguments: Bundle?
        )
    }

    /** The back stack itself, as Compose state; the last entry is on screen. */
    val backStack = mutableStateListOf<TrailSenseRoute>()

    /** Whether the latest change switched tabs (a new root) rather than going forward or back. */
    var lastChangeWasTabSwitch by mutableStateOf(false)
        private set

    @IdRes
    var startDestinationId: Int = 0
        private set

    private val listeners = mutableListOf<OnDestinationChangedListener>()

    val currentRoute: TrailSenseRoute?
        get() = backStack.lastOrNull()

    val currentDestination: TrailSenseDestination?
        get() = currentRoute?.let { destinations[it.destinationId] }

    val previousRoute: TrailSenseRoute?
        get() = backStack.getOrNull(backStack.lastIndex - 1)

    /** Starts over with only [startDestination] on the stack. */
    fun setStartDestination(@IdRes startDestination: Int, arguments: Bundle? = null) {
        startDestinationId = startDestination
        val destination = requireDestination(startDestination)
        backStack.clear()
        backStack.add(TrailSenseRoute(destination.id, destination.arguments(arguments)))
        lastChangeWasTabSwitch = true
        notifyChanged()
    }

    fun navigate(@IdRes id: Int, arguments: Bundle? = null, options: TrailSenseNavOptions? = null) {
        val destination = requireDestination(resolve(id))

        if (options?.popUpTo != null) {
            val index = backStack.indexOfLast { it.destinationId == options.popUpTo }
            if (index >= 0) {
                val keep = if (options.popUpToInclusive) index else index + 1
                while (backStack.size > keep) backStack.removeAt(backStack.lastIndex)
            }
        }

        val route = TrailSenseRoute(destination.id, destination.arguments(arguments))
        val top = backStack.lastOrNull()
        if (options?.launchSingleTop == true && top?.destinationId == destination.id) {
            backStack[backStack.lastIndex] = route.copy(key = top.key)
        } else {
            backStack.add(route)
        }
        lastChangeWasTabSwitch = options?.popUpTo != null && backStack.size <= 2
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

    fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        listeners += listener
        val destination = currentDestination ?: return
        listener.onDestinationChanged(this, destination, currentRoute?.arguments)
    }

    fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        listeners -= listener
    }

    fun saveState(): Bundle = Bundle().apply {
        putInt(KEY_START, startDestinationId)
        putParcelableArrayList(KEY_STACK, ArrayList(backStack))
    }

    /** Restores a stack saved by [saveState]; false when there was nothing usable to restore. */
    fun restoreState(state: Bundle?): Boolean {
        state ?: return false
        val stack = BundleCompat.getParcelableArrayList(state, KEY_STACK, TrailSenseRoute::class.java)
            ?.filter { destinations.containsKey(it.destinationId) }
        if (stack.isNullOrEmpty()) return false
        startDestinationId = state.getInt(KEY_START, stack.first().destinationId)
        backStack.clear()
        backStack.addAll(stack)
        lastChangeWasTabSwitch = true
        notifyChanged()
        return true
    }

    /** An old action id ("from this screen to that one") becomes its target; a destination id stays itself. */
    @IdRes
    private fun resolve(@IdRes id: Int): Int {
        if (destinations.containsKey(id)) return id
        currentDestination?.actions?.get(id)?.let { return it }
        return destinations.values.firstNotNullOfOrNull { it.actions[id] } ?: id
    }

    private fun requireDestination(@IdRes id: Int): TrailSenseDestination {
        return destinations[id] ?: throw IllegalArgumentException("Unknown destination $id")
    }

    private fun notifyChanged() {
        val destination = currentDestination ?: return
        val arguments = currentRoute?.arguments
        listeners.toList().forEach { it.onDestinationChanged(this, destination, arguments) }
    }

    private companion object {
        const val KEY_START = "start"
        const val KEY_STACK = "stack"
    }
}
