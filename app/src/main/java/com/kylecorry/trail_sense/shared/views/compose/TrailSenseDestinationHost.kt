package com.kylecorry.trail_sense.shared.views.compose

import com.kylecorry.trail_sense.shared.navigation.Route

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * The shown screen's link to the app: its context, the navigator, its lifecycle and route, and the platform
 * [ScreenEffects]. Screens themselves read these from the composition (LocalContext, LocalNavigator,
 * LocalLifecycleOwner, LocalScreenEffects); the host is what quick actions, volume actions and the activity reach.
 */
interface TrailSenseDestinationHost : ScreenEffects {
    val context: Context
    val navigator: TrailSenseNavigator
    val lifecycleOwner: LifecycleOwner
    /** The screen this host shows, with its arguments. */
    val route: Route

    /**
     * What the shown screen offers the activity while it is on top, such as a FlashlightController for the volume
     * buttons or a ToolSearchFocusable for reselecting the Tools tab. Screens set it with [ScreenControllerEffect];
     * null when the screen offers nothing.
     */
    var screenController: Any?

    fun openWidgets()
    fun reloadTheme()
    fun getBottomNavigationHeight(): Int
    fun addOverlayView(view: View)
    fun removeOverlayView(view: View)
}

interface TrailSenseReactiveDestinationHost : TrailSenseDestinationHost {
    val hookResetOnResume: Int
    fun cleanupReactiveEffects()
}

/** This screen's route, as the type the screen was registered for. */
inline fun <reified T : Route> TrailSenseDestinationHost.routeAs(): T = route as T

/** The shown screen's controller, if it offers a [T]. */
inline fun <reified T> TrailSenseDestinationHost.screenControllerAs(): T? = screenController as? T

/** Offers [controller] as this screen's [TrailSenseDestinationHost.screenController] while the screen is shown. */
@Composable
fun ScreenControllerEffect(host: TrailSenseDestinationHost, controller: Any) {
    DisposableEffect(host, controller) {
        host.screenController = controller
        onDispose {
            if (host.screenController === controller) {
                host.screenController = null
            }
        }
    }
}
