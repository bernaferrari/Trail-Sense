package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.trail_sense.shared.navigation.Route
import android.app.Activity
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.findNavigator
import java.lang.ref.WeakReference

class NavigationScreenLock(private val alwaysLock: Boolean = false) {

    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val shouldLock by lazy { prefs.navigation.keepScreenUnlockedWhileNavigating }
    private val navigator = DependencyRegistry.get<Navigator>()

    private var activityReference: WeakReference<Activity>? = null
    private var selfRoute: Route? = null
    private val listener = TrailSenseNavigator.OnRouteChangedListener { _, route ->
        if (route != selfRoute) {
            releaseLock(activityReference?.get() ?: return@OnRouteChangedListener)
        }
    }
    private val removeListenerTimer = CoroutineTimer {
        tryOrNothing {
            activityReference?.get()?.let {
                removeNavigationListener(it)
            }
        }
    }

    fun updateLock(activity: Activity) {
        updateLock(
            activity,
            navigator.getBeaconDestinationId() != null || PathNavigationStore().hasSavedRoute()
        )
    }

    fun updateLock(activity: Activity, isNavigating: Boolean) {
        activityReference = WeakReference(activity)
        tryOrNothing {
            val shouldShowWhenLocked = alwaysLock || (shouldLock && isNavigating)
            if (shouldShowWhenLocked) {
                Screen.setShowWhenLocked(activity, true)
                addNavigationListener(activity)
            } else {
                releaseLock(activity)
            }
        }
    }

    fun releaseLock(activity: Activity) {
        tryOrNothing {
            Screen.setShowWhenLocked(activity, false)
            removeListenerTimer.once(1000)
        }
    }

    private fun removeNavigationListener(activity: Activity) {
        val mainActivity = activity as? MainActivity ?: return
        val appNavigator = mainActivity.findNavigator() ?: return
        appNavigator.removeOnRouteChangedListener(listener)
    }

    private fun addNavigationListener(activity: Activity) {
        val weakActivity = WeakReference(activity)
        val mainActivity = weakActivity.get() as? MainActivity ?: return
        val appNavigator = mainActivity.findNavigator()
        selfRoute = appNavigator?.currentRoute
        appNavigator?.addOnRouteChangedListener(listener)
    }

}
