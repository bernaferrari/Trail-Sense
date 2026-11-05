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

interface TrailSenseDestinationHost {
    val lifecycleOwner: LifecycleOwner
    val lifecycleScope: CoroutineScope
    val mainActivity: MainActivity
    val intentResultRetriever: IntentResultRetriever
    /** The screen this host shows, with its arguments. */
    val route: Route

    fun requireContext(): Context
    fun getResources(): Resources
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String
    fun startActivity(intent: Intent)
    fun findNavigator(): TrailSenseNavigator
    fun toast(message: CharSequence, short: Boolean = true)
    fun inBackground(
        state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
        cancelWhenRerun: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ): Job
    fun requestCamera(action: (hasPermission: Boolean) -> Unit)
    fun requestActivityRecognition(action: (hasPermission: Boolean) -> Unit)
    fun requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit)
    fun requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit)
    fun requestBackgroundLocation(action: (hasPermission: Boolean) -> Unit)
    fun requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit)
    fun alertNoCameraPermission()
    fun alertNoActivityRecognitionPermission()
    fun showPhotoImport(
        resolution: Size?,
        useZeroShutterLag: Boolean,
        onCapture: (Uri?) -> Unit
    )

    fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        onAction: (ActionItem?) -> Unit
    )

    fun showSnackbar(
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar

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
